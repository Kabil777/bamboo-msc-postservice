package com.bamboo.postService.service;

import com.bamboo.postService.common.enums.Roles;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.common.helper.PostServiceHelper;
import com.bamboo.postService.common.response.RoleResponse;
import com.bamboo.postService.dto.common.AuthorSummaryV1Dto;
import com.bamboo.postService.dto.doc.DocCursorResponseV1Dto;
import com.bamboo.postService.dto.doc.DocDetailV1Dto;
import com.bamboo.postService.dto.doc.DocFeedItemV1Dto;
import com.bamboo.postService.dto.doc.DocHomeDto;
import com.bamboo.postService.dto.doc.DocPageAccessView;
import com.bamboo.postService.entity.Docs;
import com.bamboo.postService.entity.DocsMember;
import com.bamboo.postService.entity.Tags;
import com.bamboo.postService.exception.RoleNotFoundException;
import com.bamboo.postService.policy.PostAccessPolicy;
import com.bamboo.postService.repository.DocsRepository;
import com.bamboo.postService.repository.DocsRoleRepository;
import com.bamboo.postService.repository.PageRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocsQueryService {

    private final DocsRepository docsRepository;
    private final PageRepository pageRepository;
    private final DocsRoleRepository docsRoleRepository;
    private final PostAccessPolicy postAccessPolicy;

    public DocsQueryService(
            DocsRepository docsRepository,
            PageRepository pageRepository,
            DocsRoleRepository docsRoleRepository,
            PostAccessPolicy postAccessPolicy) {
        this.docsRepository = docsRepository;
        this.pageRepository = pageRepository;
        this.docsRoleRepository = docsRoleRepository;
        this.postAccessPolicy = postAccessPolicy;
    }

    @Transactional
    public DocDetailV1Dto getDocAndContent(UUID id, UUID userId) {
        Docs docs =
                docsRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Document with current Id not found"));

        postAccessPolicy.assertCanView(
                docs.getVisibility(), docs.getStatus(), resolveDocsRole(id, userId));

        Set<String> tags = docs.getTags().stream().map(Tags::getTag).collect(Collectors.toSet());

        DocDetailV1Dto response =
                new DocDetailV1Dto(
                        docs.getId(),
                        docs.getTitle(),
                        docs.getCoverUrl(),
                        docs.getDescription(),
                        docs.getContent(),
                        tags,
                        docs.getCreatedAt(),
                        docs.getVisibility(),
                        docs.getStatus(),
                        docs.getTree(),
                        PostServiceHelper.authorMapper(docs.getAuthorSnapshot()));
        return response;
    }

    @Transactional
    public String getPageWithId(UUID pageId, UUID docId, UUID userId) {
        DocPageAccessView page =
                pageRepository
                        .findPageAccessView(pageId, docId)
                        .orElseThrow(() -> new EntityNotFoundException("Page Not Found"));

        postAccessPolicy.assertCanView(
                page.getVisibility(), page.getStatus(), resolveDocsRole(docId, userId));
        return page.getContent();
    }

    public RoleResponse getRole(UUID userId, UUID docsId) {
        DocsMember role =
                docsRoleRepository
                        .findByDocsIdAndUserId(docsId, userId)
                        .orElseThrow(() -> new RoleNotFoundException("Unauthorized"));

        return new RoleResponse(
                true,
                role.getRole().name(),
                role.getRole() == Roles.READER,
                org.springframework.http.HttpStatus.OK,
                "has reader access to this document");
    }

    @Transactional
    public List<DocFeedItemV1Dto> getDocForHome(Pageable pageable) {
        List<DocHomeDto> coverDocs = docsRepository.findAllCoverDocs(Visibility.PUBLIC, pageable);
        return mapDocFeedItems(coverDocs);
    }

    public DocCursorResponseV1Dto getForUser(UUID id, Instant cursor, Pageable pageable) {
        int pageSize = pageable.getPageSize();
        Pageable limitPlusOne = PageRequest.of(0, pageSize + 1, Sort.by("createdAt").descending());

        List<DocHomeDto> base = docsRepository.findForMember(id, cursor, limitPlusOne);

        boolean hasNext = base.size() > pageSize;
        if (hasNext) {
            base = base.subList(0, pageSize);
        }

        Instant nextCursor = hasNext ? base.get(base.size() - 1).createdAt() : null;
        return new DocCursorResponseV1Dto(mapDocFeedItems(base), hasNext, nextCursor);
    }

    public DocCursorResponseV1Dto getByUser(
            UUID id,
            Instant cursor,
            Pageable pageable,
            Visibility visibility,
            String requesterIdHeader) {
        int pageSize = pageable.getPageSize();
        Pageable limitPlusOne = PageRequest.of(0, pageSize + 1, Sort.by("createdAt").descending());

        visibility = postAccessPolicy.resolveRequestedVisibility(id, visibility, requesterIdHeader);

        List<DocHomeDto> base =
                docsRepository.findForAuthorWithVisibility(id, visibility, cursor, limitPlusOne);

        boolean hasNext = base.size() > pageSize;
        if (hasNext) {
            base = base.subList(0, pageSize);
        }

        Instant nextCursor = hasNext ? base.get(base.size() - 1).createdAt() : null;
        return new DocCursorResponseV1Dto(mapDocFeedItems(base), hasNext, nextCursor);
    }

    private Roles resolveDocsRole(UUID docsId, UUID userId) {
        if (userId == null) {
            return null;
        }

        return docsRoleRepository.findByDocsIdAndUserId(docsId, userId)
                .map(DocsMember::getRole)
                .orElse(null);
    }

    private List<DocFeedItemV1Dto> mapDocFeedItems(List<DocHomeDto> docs) {
        return docs.stream()
                .map(
                        doc ->
                                new DocFeedItemV1Dto(
                                        doc.id(),
                                        doc.title(),
                                        doc.coverUrl(),
                                        doc.description(),
                                        doc.createdAt(),
                                        doc.visibility(),
                                        doc.status(),
                                        new AuthorSummaryV1Dto(
                                                doc.authorId(),
                                                doc.authorName(),
                                                doc.authorHandle(),
                                                doc.authorAvatarUrl())))
                .toList();
    }
}
