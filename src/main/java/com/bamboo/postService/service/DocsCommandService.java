package com.bamboo.postService.service;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Roles;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.common.helper.DocsTrasformer;
import com.bamboo.postService.common.helper.DocsTrasformer.TransformResult;
import com.bamboo.postService.common.model.PageNode;
import com.bamboo.postService.dto.blog.MetaPostDto;
import com.bamboo.postService.dto.common.VisibilityUpdateRequest;
import com.bamboo.postService.dto.doc.DocCreateRequestDto;
import com.bamboo.postService.dto.doc.DocPageContentRequest;
import com.bamboo.postService.dto.doc.DocsContentRequest;
import com.bamboo.postService.dto.feign.UserMetaDto;
import com.bamboo.postService.entity.AuthorSnapshot;
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

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
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

    public DocsCommandService(
            DocsRepository docsRepository,
            PageRepository pageRepository,
            TagRepository tagRepository,
            DocsRoleRepository docsRoleRepository,
            UserServiceClient userServiceClient,
            PostAccessPolicy postAccessPolicy) {
        this.docsRepository = docsRepository;
        this.pageRepository = pageRepository;
        this.tagRepository = tagRepository;
        this.docsRoleRepository = docsRoleRepository;
        this.userServiceClient = userServiceClient;
        this.postAccessPolicy = postAccessPolicy;
    }

    @Transactional
    public String savePost(DocCreateRequestDto doc, UUID userId) {
        UserMetaDto actor = resolveUserById(userId);
        AuthorSnapshot snapshot =
                new AuthorSnapshot(actor.id(), actor.name(), actor.handle(), actor.coverUrl());

        Docs docs = new Docs();
        docs.setId(UUID.randomUUID());
        docs.setTitle(doc.title());
        docs.setDescription(doc.description());
        docs.setCoverUrl(doc.coverUrl());
        docs.setAuthorSnapshot(snapshot);
        docs.setContent(doc.content());
        docs.setVisibility(Visibility.PRIVATE);
        docs.setStatus(PostStatus.DRAFT);

        List<Tags> existingTags = tagRepository.findByTagIn(doc.tags());

        Set<String> requestedTags = new HashSet<>(doc.tags());
        Set<String> foundTags = existingTags.stream().map(Tags::getTag).collect(Collectors.toSet());

        requestedTags.removeAll(foundTags);

        if (!requestedTags.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid tags: " + requestedTags);
        }
        docs.setTags(new HashSet<>(existingTags));

        TransformResult result = DocsTrasformer.transform(docs.getId(), doc.pages());
        docs.setTree(result.tree());
        docsRepository.save(docs);
        docsRoleRepository.save(buildOwnerRole(docs.getId(), snapshot, actor.email()));
        pageRepository.saveAll(result.pages());
        return "Docs created successfully !";
    }

    @Transactional
    public Map<String, UUID> saveDocsMeta(MetaPostDto entity, UUID userId) {
        UserMetaDto actor = resolveUserById(userId);
        AuthorSnapshot snapshot =
                new AuthorSnapshot(actor.id(), actor.name(), actor.handle(), actor.coverUrl());
        UUID docId = UUID.randomUUID();
        UUID overviewPageId = docId;

        Docs doc =
                Docs.builder()
                        .id(docId)
                        .title(entity.title())
                        .coverUrl(entity.coverUrl())
                        .description(entity.description())
                        .authorSnapshot(snapshot)
                        .createdAt(Instant.now())
                        .visibility(Visibility.PRIVATE)
                        .status(PostStatus.DRAFT)
                        .tree(List.of(new PageNode(overviewPageId, "Overview", "", List.of())))
                        .build();
        List<Tags> existingTags = tagRepository.findByTagIn(entity.tags());
        Set<Tags> tags = new HashSet<>(existingTags);
        doc.setTags(tags);

        docsRepository.save(doc);
        docsRoleRepository.save(buildOwnerRole(doc.getId(), snapshot, actor.email()));
        pageRepository.save(new Pages(overviewPageId, doc.getId(), ""));

        return Map.of("id", doc.getId());
    }

    @Transactional
    public String saveDocsContent(UUID userId, UUID docsId, DocsContentRequest request) {
        assertCanEdit(docsId, userId);

        Docs docs =
                docsRepository
                        .findById(docsId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Document with current Id not found"));

        docs.setTree(request.tree());
        if (request.visibility() != null) {
            docs.setVisibility(request.visibility());
        }
        if (request.status() != null) {
            docs.setStatus(request.status());
        } else if (request.visibility() != null) {
            docs.setStatus(PostStatus.PUBLISHED);
        }
        docsRepository.save(docs);

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

        return "Docs content saved";
    }

    @Transactional
    public String updateVisibility(UUID docsId, UUID userId, VisibilityUpdateRequest request) {
        postAccessPolicy.assertCanManage(
                resolveDocsRole(docsId, userId), "Only owner can manage docs visibility");

        Docs docs =
                docsRepository
                        .findById(docsId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Document with current Id not found"));

        if (request.visibility() != null) {
            docs.setVisibility(request.visibility());
        }
        if (request.status() != null) {
            docs.setStatus(request.status());
        }

        docsRepository.save(docs);
        return "ok";
    }

    private DocsMember buildOwnerRole(UUID docsId, AuthorSnapshot snapshot, String email) {
        DocsMember member = new DocsMember();
        member.setDocsId(docsId);
        member.setUserId(snapshot.getId());
        member.setUserName(snapshot.getName());
        member.setUserHandle(snapshot.getHandle());
        member.setUserCoverUrl(snapshot.getAvatarUrl());
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

        return docsRoleRepository.findByDocsIdAndUserId(docsId, userId)
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
