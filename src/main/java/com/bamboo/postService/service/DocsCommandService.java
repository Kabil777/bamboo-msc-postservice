package com.bamboo.postService.service;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Roles;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.common.helper.DocsTrasformer;
import com.bamboo.postService.common.helper.DocsTrasformer.TransformResult;
import com.bamboo.postService.common.model.PageNode;
import com.bamboo.postService.dto.collab.CollabDeleteEvent;
import com.bamboo.postService.dto.blog.MetaPostDto;
import com.bamboo.postService.dto.common.VisibilityUpdateRequest;
import com.bamboo.postService.dto.count.ContentCountEvent;
import com.bamboo.postService.dto.doc.DocCreateRequestDto;
import com.bamboo.postService.dto.doc.DocPageContentRequest;
import com.bamboo.postService.dto.doc.DocsContentRequest;
import com.bamboo.postService.dto.feign.UserMetaDto;
import com.bamboo.postService.entity.AuthorProfileProjection;
import com.bamboo.postService.entity.Docs;
import com.bamboo.postService.entity.DocsMember;
import com.bamboo.postService.entity.Pages;
import com.bamboo.postService.entity.Tags;
import com.bamboo.postService.exception.UserNotFoundException;
import com.bamboo.postService.feign.UserServiceClient;
import com.bamboo.postService.policy.PostAccessPolicy;
import com.bamboo.postService.repository.DocsRepository;
import com.bamboo.postService.repository.DocsRoleRepository;
import com.bamboo.postService.repository.PageRepository;
import com.bamboo.postService.repository.TagRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocsCommandService {

    private final DocsRepository docsRepository;
    private final PageRepository pageRepository;
    private final TagRepository tagRepository;
    private final DocsRoleRepository docsRoleRepository;
    private final UserServiceClient userServiceClient;
    private final PostAccessPolicy postAccessPolicy;
    private final AuthorProjectionService authorProjectionService;
    private final RabbitTemplate rabbitTemplate;

    public DocsCommandService(
            DocsRepository docsRepository,
            PageRepository pageRepository,
            TagRepository tagRepository,
            DocsRoleRepository docsRoleRepository,
            UserServiceClient userServiceClient,
            PostAccessPolicy postAccessPolicy,
            AuthorProjectionService authorProjectionService,
            RabbitTemplate rabbitTemplate) {
        this.docsRepository = docsRepository;
        this.pageRepository = pageRepository;
        this.tagRepository = tagRepository;
        this.docsRoleRepository = docsRoleRepository;
        this.userServiceClient = userServiceClient;
        this.postAccessPolicy = postAccessPolicy;
        this.authorProjectionService = authorProjectionService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public String savePost(DocCreateRequestDto doc, UUID userId) {
        UserMetaDto actor = resolveUserById(userId);
        AuthorProfileProjection authorProfile = authorProjectionService.upsert(actor);

        Docs docs = new Docs();
        docs.setTitle(doc.title());
        docs.setDescription(doc.description());
        docs.setCoverUrl(doc.coverUrl());
        docs.setAuthorProfile(authorProfile);
        docs.setContent(doc.content());
        docs.setVisibility(Visibility.PRIVATE);
        docs.setStatus(PostStatus.DRAFT);
        docs.setId(null);

        List<Tags> existingTags = tagRepository.findByTagIn(doc.tags());

        Set<String> requestedTags = new HashSet<>(doc.tags());
        Set<String> foundTags = existingTags.stream().map(Tags::getTag).collect(Collectors.toSet());

        requestedTags.removeAll(foundTags);

        if (!requestedTags.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid tags: " + requestedTags);
        }
        docs.setTags(new HashSet<>(existingTags));

        docs = docsRepository.saveAndFlush(docs);
        TransformResult result = DocsTrasformer.transform(docs.getId(), doc.pages());
        docs.setTree(result.tree());
        docsRepository.save(docs);
        docsRoleRepository.save(buildOwnerRole(docs.getId(), authorProfile, actor.email()));
        pageRepository.saveAll(result.pages());
        publishContentCountCreate(authorProfile.getId(), "DOCS", docs.getVisibility());
        return "Docs created successfully !";
    }

    @Transactional
    public Map<String, UUID> saveDocsMeta(MetaPostDto entity, UUID userId) {
        UserMetaDto actor = resolveUserById(userId);
        AuthorProfileProjection authorProfile = authorProjectionService.upsert(actor);

        Docs doc =
                Docs.builder()
                        .title(entity.title())
                        .coverUrl(entity.coverUrl())
                        .description(entity.description())
                        .authorProfile(authorProfile)
                        .visibility(Visibility.PRIVATE)
                        .status(PostStatus.DRAFT)
                        .build();
        List<Tags> existingTags = tagRepository.findByTagIn(entity.tags());
        Set<Tags> tags = new HashSet<>(existingTags);
        doc.setTags(tags);
        doc.setId(null);

        doc = docsRepository.saveAndFlush(doc);
        UUID overviewPageId = doc.getId();
        doc.setTree(List.of(new PageNode(overviewPageId, "Overview", "", List.of())));
        docsRepository.save(doc);
        docsRoleRepository.save(buildOwnerRole(doc.getId(), authorProfile, actor.email()));
        pageRepository.save(new Pages(overviewPageId, doc.getId(), ""));
        publishContentCountCreate(authorProfile.getId(), "DOCS", doc.getVisibility());

        return Map.of("id", doc.getId());
    }

    @Transactional
    public String saveDocsContent(UUID userId, UUID docsId, DocsContentRequest request) {
        assertCanEdit(docsId, userId);

        Docs docs =
                docsRepository
                        .findByIdForUpdate(docsId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Document with current Id not found"));

        Visibility previousVisibility = docs.getVisibility();
        docs.setTree(request.tree());
        List<PageNode> strippedTree = DocsTrasformer.stripTreeContent(request.tree());
        docs.setTree(strippedTree);
        if (request.visibility() != null) {
            docs.setVisibility(request.visibility());
        }
        if (request.status() != null) {
            docs.setStatus(request.status());
        } else if (request.visibility() != null) {
            docs.setStatus(PostStatus.PUBLISHED);
        }
        if (request.pages() != null && !request.pages().isEmpty()) {
            request.pages().stream()
                    .filter(page -> Objects.equals(page.pageId(), docsId))
                    .map(DocPageContentRequest::markdown)
                    .findFirst()
                    .ifPresent(docs::setContent);

            List<Pages> pages =
                    request.pages().stream()
                            .map(
                                    (DocPageContentRequest page) ->
                                            new Pages(page.pageId(), docsId, page.markdown()))
                            .toList();
            pageRepository.saveAll(pages);
        }

        if ((request.visibility() != null || request.status() != null)
                && !StringUtils.hasText(docs.getContent())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Content is required before visibility/status update");
        }

        publishContentCountEvent(
                docs.getAuthorProfile().getId(), "DOCS", previousVisibility, docs.getVisibility());

        return "Docs content saved";
    }

    @Transactional
    public String updateVisibility(UUID docsId, UUID userId, VisibilityUpdateRequest request) {
        postAccessPolicy.assertCanManage(
                resolveDocsRole(docsId, userId), "Only owner can manage docs visibility");

        Docs docs =
                docsRepository
                        .findByIdForUpdate(docsId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Document with current Id not found"));

        Visibility previousVisibility = docs.getVisibility();
        if (!StringUtils.hasText(docs.getContent())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot change visibility for empty content");
        }

        if (request.visibility() != null) {
            docs.setVisibility(request.visibility());
        }
        if (request.status() != null) {
            docs.setStatus(request.status());
        }

        publishContentCountEvent(
                docs.getAuthorProfile().getId(), "DOCS", previousVisibility, docs.getVisibility());

        return "ok";
    }

    @Transactional
    public void deleteById(UUID docsId, UUID userId) {
        postAccessPolicy.assertCanManage(
                resolveDocsRole(docsId, userId), "Only owner can delete docs");

        Docs docs =
                docsRepository
                        .findByIdForUpdate(docsId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Document with current Id not found"));

        UUID authorId = docs.getAuthorProfile().getId();
        Visibility visibility = docs.getVisibility();

        pageRepository.deleteAllByDocId(docsId);
        docsRoleRepository.deleteAllByDocsId(docsId);
        docsRepository.delete(docs);

        publishContentCountDelete(authorId, "DOCS", visibility);
        publishCollabDeleteEvent("DOCUMENT", docsId);
    }

    private void publishContentCountCreate(UUID userId, String contentType, Visibility visibility) {
        publishAfterCommit(new ContentCountEvent(userId, contentType, "CREATED", null, visibility));
    }

    private void publishContentCountDelete(UUID userId, String contentType, Visibility visibility) {
        publishAfterCommit(new ContentCountEvent(userId, contentType, "DELETED", visibility, null));
    }

    private void publishContentCountEvent(
            UUID userId, String contentType, Visibility oldVisibility, Visibility newVisibility) {
        if (oldVisibility == null || newVisibility == null || oldVisibility == newVisibility) {
            return;
        }

        publishAfterCommit(
                new ContentCountEvent(
                        userId, contentType, "VISIBILITY_CHANGED", oldVisibility, newVisibility));
    }

    private void publishCollabDeleteEvent(String contentType, UUID contentId) {
        publishAfterCommit(
                () ->
                        rabbitTemplate.convertAndSend(
                                "collab.events",
                                "collab.document.deleted",
                                new CollabDeleteEvent(contentType, contentId)));
    }

    private void publishAfterCommit(ContentCountEvent event) {
        publishAfterCommit(
                () -> rabbitTemplate.convertAndSend("content.events", "content.counts", event));
    }

    private void publishAfterCommit(Runnable publisher) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publisher.run();
                    }
                });
    }

    private DocsMember buildOwnerRole(
            UUID docsId, AuthorProfileProjection authorProfile, String email) {
        DocsMember member = new DocsMember();
        member.setDocsId(docsId);
        member.setUserId(authorProfile.getId());
        member.setUserName(authorProfile.getName());
        member.setUserHandle(authorProfile.getHandle());
        member.setUserCoverUrl(authorProfile.getAvatarUrl());
        member.setUserEmail(email);
        member.setRole(Roles.OWNER);
        return member;
    }

    private void assertCanEdit(UUID docsId, UUID userId) {
        postAccessPolicy.assertCanEdit(resolveDocsRole(docsId, userId));
    }

    private Roles resolveDocsRole(UUID docsId, UUID userId) {
        if (userId == null) {
            return null;
        }

        return docsRoleRepository
                .findByDocsIdAndUserId(docsId, userId)
                .map(DocsMember::getRole)
                .orElse(null);
    }

    private UserMetaDto resolveUserById(UUID userId) {
        try {
            return userServiceClient.getUserById(userId);
        } catch (Exception ex) {
            throw new UserNotFoundException("User not found: " + userId);
        }
    }
}
