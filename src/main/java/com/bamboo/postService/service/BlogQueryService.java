package com.bamboo.postService.service;

import com.bamboo.postService.common.enums.Roles;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.common.helper.PostServiceHelper;
import com.bamboo.postService.common.response.RoleResponse;
import com.bamboo.postService.dto.blog.BlogCursorResponseV1Dto;
import com.bamboo.postService.dto.blog.BlogDetailV1Dto;
import com.bamboo.postService.dto.blog.BlogFeedItemV1Dto;
import com.bamboo.postService.dto.blog.BlogPageBase;
import com.bamboo.postService.dto.blog.BlogTagView;
import com.bamboo.postService.dto.common.AuthorSummaryV1Dto;
import com.bamboo.postService.entity.Blog;
import com.bamboo.postService.entity.BlogContent;
import com.bamboo.postService.entity.BlogMember;
import com.bamboo.postService.exception.RoleNotFoundException;
import com.bamboo.postService.policy.PostAccessPolicy;
import com.bamboo.postService.repository.BlogContentRepository;
import com.bamboo.postService.repository.BlogRepository;
import com.bamboo.postService.repository.BlogRoleRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BlogQueryService {

    private final BlogRepository blogRepository;
    private final BlogContentRepository blogContentRepository;
    private final BlogRoleRepository blogRoleRepository;
    private final PostAccessPolicy postAccessPolicy;

    public BlogQueryService(
            BlogRepository blogRepository,
            BlogContentRepository blogContentRepository,
            BlogRoleRepository blogRoleRepository,
            PostAccessPolicy postAccessPolicy) {
        this.blogRepository = blogRepository;
        this.blogContentRepository = blogContentRepository;
        this.blogRoleRepository = blogRoleRepository;
        this.postAccessPolicy = postAccessPolicy;
    }

    public BlogCursorResponseV1Dto getCoverBlogs(Pageable pageable, Instant cursor) {
        int pageSize = pageable.getPageSize();
        Pageable limitPlusOne = PageRequest.of(0, pageSize + 1, Sort.by("createdAt").descending());

        List<BlogPageBase> base =
                blogRepository.findNextBlogPageBases(Visibility.PUBLIC, cursor, limitPlusOne);

        boolean hasNext = base.size() > pageSize;
        if (hasNext) {
            base = base.subList(0, pageSize);
        }

        Instant nextCursor = hasNext ? base.get(base.size() - 1).getCreatedAt() : null;
        List<BlogFeedItemV1Dto> pages = mapFeedItems(base);

        return new BlogCursorResponseV1Dto(pages, hasNext, nextCursor);
    }

    public List<BlogFeedItemV1Dto> getFeaturedBlogs(Pageable pageable) {
        List<BlogPageBase> base =
                blogRepository.findNextBlogPageBases(Visibility.PUBLIC, Instant.now(), pageable);
        return mapFeedItems(base);
    }

    public BlogCursorResponseV1Dto getByTags(
            Instant cursor, String tag, Pageable pageable) {
        int pageSize = pageable.getPageSize();
        Pageable limitPlusOne = PageRequest.of(0, pageSize + 1, Sort.by("createdAt").descending());

        List<BlogPageBase> base =
                blogRepository.findByTags(Visibility.PUBLIC, tag, cursor, limitPlusOne);

        boolean hasNext = base.size() > pageSize;
        if (hasNext) {
            base = base.subList(0, pageSize);
        }

        Instant nextCursor = hasNext ? base.get(base.size() - 1).getCreatedAt() : null;
        List<BlogFeedItemV1Dto> pages = mapFeedItems(base);

        return new BlogCursorResponseV1Dto(pages, hasNext, nextCursor);
    }

    public String getContent(UUID id) {
        BlogContent blog =
                blogContentRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Blog does not found !"));
        return blog.getContent();
    }

    @Transactional
    public BlogDetailV1Dto getBlogById(UUID id, UUID userId) {
        Blog blog =
                blogRepository
                        .findBlogWithContent(id)
                        .orElseThrow(() -> new EntityNotFoundException("Blog does not found"));

        Roles memberRole = resolveBlogRole(id, userId);
        postAccessPolicy.assertCanView(blog.getVisibility(), blog.getStatus(), memberRole);

        if (blog.getContent() == null) {
            throw new EntityNotFoundException("Blog content not found");
        }
        List<String> tags = blogRepository.findTagNamesByBlogId(id);
        List<AuthorSummaryV1Dto> collaborators =
                PostServiceHelper.collaboratorMapper(
                                blogRoleRepository.findAllByBlogIdInAndRoleIn(
                                        List.of(id), List.of(Roles.OWNER, Roles.EDITOR)))
                        .getOrDefault(id, List.of());
        return new BlogDetailV1Dto(
                blog.getId(),
                blog.getTitle(),
                blog.getDescription(),
                blog.getCoverUrl(),
                blog.getContent().getContent(),
                tags,
                blog.getCreatedAt(),
                blog.getVisibility(),
                blog.getStatus(),
                PostServiceHelper.authorMapper(blog.getAuthorSnapshot()),
                collaborators);
    }

    public BlogCursorResponseV1Dto getByUser(
            UUID id,
            Instant cursor,
            Pageable pageable,
            Visibility visibility,
            String requesterIdHeader) {
        int pageSize = pageable.getPageSize();
        Pageable limitPlusOne = PageRequest.of(0, pageSize + 1, Sort.by("createdAt").descending());

        visibility = postAccessPolicy.resolveRequestedVisibility(id, visibility, requesterIdHeader);

        List<BlogPageBase> base =
                blogRepository.findByAuthorId(id, visibility, cursor, limitPlusOne);

        boolean hasNext = base.size() > pageSize;
        if (hasNext) {
            base = base.subList(0, pageSize);
        }

        Instant nextCursor = hasNext ? base.get(base.size() - 1).getCreatedAt() : null;
        List<BlogFeedItemV1Dto> pages = mapFeedItems(base);

        return pages.isEmpty()
                ? new BlogCursorResponseV1Dto(List.of(), false, null)
                : new BlogCursorResponseV1Dto(pages, hasNext, nextCursor);
    }

    public BlogCursorResponseV1Dto getForUser(UUID id, Instant cursor, Pageable pageable) {
        int pageSize = pageable.getPageSize();
        Pageable limitPlusOne = PageRequest.of(0, pageSize + 1, Sort.by("createdAt").descending());

        List<BlogPageBase> base = blogRepository.findForMember(id, cursor, limitPlusOne);

        boolean hasNext = base.size() > pageSize;
        if (hasNext) {
            base = base.subList(0, pageSize);
        }

        Instant nextCursor = hasNext ? base.get(base.size() - 1).getCreatedAt() : null;
        List<BlogFeedItemV1Dto> pages = mapFeedItems(base);

        return pages.isEmpty()
                ? new BlogCursorResponseV1Dto(List.of(), false, null)
                : new BlogCursorResponseV1Dto(pages, hasNext, nextCursor);
    }

    public RoleResponse getRole(UUID userId, UUID blogId) {
        BlogMember role =
                blogRoleRepository
                        .findByBlogIdAndUserId(blogId, userId)
                        .orElseThrow(() -> new RoleNotFoundException("Unauthorized"));

        return new RoleResponse(
                true,
                role.getRole().name(),
                role.getRole() == Roles.READER,
                org.springframework.http.HttpStatus.OK,
                "has reader access to this document");
    }

    private Roles resolveBlogRole(UUID blogId, UUID userId) {
        if (userId == null) {
            return null;
        }

        return blogRoleRepository.findByBlogIdAndUserId(blogId, userId)
                .map(BlogMember::getRole)
                .orElse(null);
    }

    private List<BlogFeedItemV1Dto> mapFeedItems(List<BlogPageBase> base) {
        List<BlogTagView> tags =
                blogRepository.findTagsForBlogs(base.stream().map(BlogPageBase::getId).toList());
        Map<UUID, List<String>> tagMap = PostServiceHelper.tagMapper.apply(tags);
        Map<UUID, List<AuthorSummaryV1Dto>> collaboratorMap =
                loadCollaborators(base.stream().map(BlogPageBase::getId).toList());
        return PostServiceHelper.pageMapper(base, tagMap, collaboratorMap);
    }

    private Map<UUID, List<AuthorSummaryV1Dto>> loadCollaborators(List<UUID> blogIds) {
        if (blogIds.isEmpty()) {
            return Map.of();
        }

        return PostServiceHelper.collaboratorMapper(
                blogRoleRepository.findAllByBlogIdInAndRoleIn(
                        blogIds, List.of(Roles.OWNER, Roles.EDITOR)));
    }
}
