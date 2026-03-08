package com.bamboo.postService.service;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Roles;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.common.helper.PostServiceHelper;
import com.bamboo.postService.common.response.CommonResponse;
import com.bamboo.postService.common.response.RoleResponse;
import com.bamboo.postService.dto.blog.BlogDetailsDto;
import com.bamboo.postService.dto.blog.BlogPageBase;
import com.bamboo.postService.dto.blog.BlogCollaboratorDto;
import com.bamboo.postService.dto.blog.BlogPagesDto;
import com.bamboo.postService.dto.blog.BlogTagView;
import com.bamboo.postService.dto.blog.CursorResponse;
import com.bamboo.postService.dto.blog.MetaPostDto;
import com.bamboo.postService.dto.common.VisibilityUpdateRequest;
import com.bamboo.postService.dto.feign.UserMetaDto;
import com.bamboo.postService.entity.AuthorSnapshot;
import com.bamboo.postService.entity.Blog;
import com.bamboo.postService.entity.BlogContent;
import com.bamboo.postService.entity.BlogMember;
import com.bamboo.postService.entity.Tags;
import com.bamboo.postService.exception.AccessDeniedException;
import com.bamboo.postService.exception.RoleNotFoundException;
import com.bamboo.postService.exception.UserNotFoundException;
import com.bamboo.postService.feign.UserServiceClient;
import com.bamboo.postService.repository.BlogContentRepository;
import com.bamboo.postService.repository.BlogRepository;
import com.bamboo.postService.repository.BlogRoleRepository;
import com.bamboo.postService.repository.TagRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class BlogService extends BaseService {

    private final BlogRepository blogRepository;
    private final TagRepository tagRepository;
    private final BlogContentRepository blogContentRepository;
    private final BlogRoleRepository blogRoleRepository;
    private final UserServiceClient userServiceClient;

    public BlogService(
            BlogRepository blogRepository,
            TagRepository tagRepository,
            BlogContentRepository blogContentRepository,
            BlogRoleRepository blogRoleRepository,
            UserServiceClient userServiceClient) {
        this.blogRepository = blogRepository;
        this.tagRepository = tagRepository;
        this.blogContentRepository = blogContentRepository;
        this.blogRoleRepository = blogRoleRepository;
        this.userServiceClient = userServiceClient;
    }

    @Transactional
    public ResponseEntity<CommonResponse<String>> saveContent(
            UUID userId, UUID blogId, String content, Visibility visibility, PostStatus status) {

        assertCanEdit(blogId, userId);

        blogContentRepository.upsertContent(blogId, content);
        Blog blog =
                blogRepository
                        .findById(blogId)
                        .orElseThrow(() -> new EntityNotFoundException("Blog does not found !"));
        if (visibility != null) {
            blog.setVisibility(visibility);
        }
        if (status != null) {
            blog.setStatus(status);
        } else if (visibility != null) {
            blog.setStatus(PostStatus.PUBLISHED);
        }
        blogRepository.save(blog);
        return buildResponse(HttpStatus.CREATED, "ok");
    }

    @Transactional
    public ResponseEntity<CommonResponse<Map<String, UUID>>> save(
            MetaPostDto blogDto, UUID userId) {
        UserMetaDto actor = resolveUserById(userId);
        AuthorSnapshot authorSnapshot =
                new AuthorSnapshot(actor.id(), actor.name(), actor.handle(), actor.coverUrl());

        Blog blog =
                Blog.builder()
                        .title(blogDto.title())
                        .coverUrl(blogDto.coverUrl())
                        .description(blogDto.description())
                        .authorSnapshot(authorSnapshot)
                        .visibility(Visibility.PRIVATE)
                        .status(PostStatus.DRAFT)
                        .build();

        List<Tags> existingTags = tagRepository.findByTagIn(blogDto.tags());
        Set<Tags> tags = new HashSet<>(existingTags);
        blog.setTags(tags);

        blogRepository.save(blog);
        blogRoleRepository.save(
                BlogMember.builder()
                        .blogId(blog.getId())
                        .userId(authorSnapshot.getId())
                        .userName(authorSnapshot.getName())
                        .userHandle(authorSnapshot.getHandle())
                        .userCoverUrl(authorSnapshot.getAvatarUrl())
                        .userEmail(actor.email())
                        .role(Roles.OWNER)
                        .build());
        return buildResponse(HttpStatus.CREATED, Map.of("id", blog.getId()));
    }

    private UserMetaDto resolveUserById(UUID userId) {
        try {
            return userServiceClient.getUserById(userId);
        } catch (Exception ex) {
            throw new UserNotFoundException("User not found: " + userId);
        }
    }

    /**
     * @param pageable used to returning required amount of pages
     * @param cursor used for creating cursor pagination
     * @return page of blogs order by timestamp descending order
     */
    public ResponseEntity<CursorResponse> getCoverBlogs(Pageable pageable, Instant cursor) {
        int pageSize = pageable.getPageSize();
        Pageable limitPlusOne = PageRequest.of(0, pageSize + 1, Sort.by("createdAt").descending());

        List<BlogPageBase> base =
                blogRepository.findNextBlogPageBases(Visibility.PUBLIC, cursor, limitPlusOne);

        boolean hasNext = base.size() > pageSize;
        if (hasNext) {
            base = base.subList(0, pageSize);
        }

        Instant nextCursor = hasNext ? base.get(base.size() - 1).getCreatedAt() : null;

        List<BlogTagView> tags =
                blogRepository.findTagsForBlogs(base.stream().map(BlogPageBase::getId).toList());

        Map<UUID, List<String>> tagMap = PostServiceHelper.tagMapper.apply(tags);
        Map<UUID, List<BlogCollaboratorDto>> collaboratorMap = loadCollaborators(base);
        List<BlogPagesDto> pages = PostServiceHelper.pageMapper(base, tagMap, collaboratorMap);

        if (pages.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(new CursorResponse(pages, hasNext, nextCursor));
    }

    public ResponseEntity<List<BlogPagesDto>> getFeaturedBlogs(Pageable pageable) {
        List<BlogPageBase> base =
                blogRepository.findNextBlogPageBases(Visibility.PUBLIC, Instant.now(), pageable);

        List<BlogTagView> tags =
                blogRepository.findTagsForBlogs(base.stream().map(BlogPageBase::getId).toList());

        Map<UUID, List<String>> tagMap = PostServiceHelper.tagMapper.apply(tags);
        Map<UUID, List<BlogCollaboratorDto>> collaboratorMap = loadCollaborators(base);
        List<BlogPagesDto> pages = PostServiceHelper.pageMapper(base, tagMap, collaboratorMap);

        return ResponseEntity.ok(pages);
    }

    public ResponseEntity<CursorResponse> getByTags(Instant cursor, String tag, Pageable pageable) {
        int pageSize = pageable.getPageSize();
        Pageable limitPlusOne = PageRequest.of(0, pageSize + 1, Sort.by("createdAt").descending());

        List<BlogPageBase> base =
                blogRepository.findByTags(Visibility.PUBLIC, tag, cursor, limitPlusOne);

        boolean hasNext = base.size() > pageSize;
        if (hasNext) {
            base = base.subList(0, pageSize);
        }

        Instant nextCursor = hasNext ? base.get(base.size() - 1).getCreatedAt() : null;

        List<BlogTagView> tags =
                blogRepository.findTagsForBlogs(base.stream().map(BlogPageBase::getId).toList());

        Map<UUID, List<String>> tagMap = PostServiceHelper.tagMapper.apply(tags);
        Map<UUID, List<BlogCollaboratorDto>> collaboratorMap = loadCollaborators(base);
        List<BlogPagesDto> pages = PostServiceHelper.pageMapper(base, tagMap, collaboratorMap);

        if (pages.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(new CursorResponse(pages, hasNext, nextCursor));
    }

    /**
     * @param id id of the post to be quried
     * @return page content of the blog
     */
    public ResponseEntity<CommonResponse<String>> getContent(UUID id) {
        BlogContent blog =
                blogContentRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Blog does not found !"));
        return buildResponse(HttpStatus.OK, blog.getContent());
    }

    @Transactional()
    public ResponseEntity<BlogDetailsDto> getBlogById(UUID id, UUID userId) {
        Blog blog =
                blogRepository
                        .findBlogWithContent(id)
                        .orElseThrow(() -> new EntityNotFoundException("Blog does not found"));

        if (!hasViewAccess(blog, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        if (blog.getContent() == null) {
            throw new EntityNotFoundException("Blog content not found");
        }
        List<String> tags = blogRepository.findTagNamesByBlogId(id);
        List<BlogCollaboratorDto> collaborators =
                PostServiceHelper.collaboratorMapper(
                                blogRoleRepository.findAllByBlogIdInAndRoleIn(
                                        List.of(id), List.of(Roles.OWNER, Roles.EDITOR)))
                        .getOrDefault(id, List.of());
        return ResponseEntity.ok(
                new BlogDetailsDto(
                        blog.getId(),
                        blog.getTitle(),
                        blog.getDescription(),
                        blog.getCoverUrl(),
                        blog.getContent().getContent(),
                        blog.getCreatedAt(),
                        tags,
                        blog.getAuthorSnapshot(),
                        collaborators));
    }

    private boolean hasViewAccess(Blog blog, UUID userId) {
        if (blog.getVisibility() == Visibility.PUBLIC && blog.getStatus() == PostStatus.PUBLISHED) {
            return true;
        }

        if (userId == null) {
            return false;
        }

        return blogRoleRepository.findByBlogIdAndUserId(blog.getId(), userId).isPresent();
    }

    public ResponseEntity<CursorResponse> getByUser(
            UUID id, Instant cursor, Pageable pageable, Visibility visibility, String requesterIdHeader) {
        int pageSize = pageable.getPageSize();
        Pageable limitPlusOne = PageRequest.of(0, pageSize + 1, Sort.by("createdAt").descending());

        visibility = resolveRequestedVisibility(id, visibility, requesterIdHeader);

        List<BlogPageBase> base =
                blogRepository.findByAuthorId(id, visibility, cursor, limitPlusOne);

        boolean hasNext = base.size() > pageSize;
        if (hasNext) {
            base = base.subList(0, pageSize);
        }

        Instant nextCursor = hasNext ? base.get(base.size() - 1).getCreatedAt() : null;

        List<BlogTagView> tags =
                blogRepository.findTagsForBlogs(base.stream().map(BlogPageBase::getId).toList());

        Map<UUID, List<String>> tagMap = PostServiceHelper.tagMapper.apply(tags);
        Map<UUID, List<BlogCollaboratorDto>> collaboratorMap = loadCollaborators(base);
        List<BlogPagesDto> pages = PostServiceHelper.pageMapper(base, tagMap, collaboratorMap);

        if (pages.isEmpty()) {
            return ResponseEntity.ok(new CursorResponse(List.of(), false, null));
        }

        return ResponseEntity.ok(new CursorResponse(pages, hasNext, nextCursor));
    }

    private Visibility resolveRequestedVisibility(
            UUID resourceOwnerId, Visibility requestedVisibility, String requesterIdHeader) {
        if (requestedVisibility == null || requestedVisibility == Visibility.PUBLIC) {
            return Visibility.PUBLIC;
        }

        if (requesterIdHeader == null || requesterIdHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        UUID requesterId;
        try {
            requesterId = UUID.fromString(requesterIdHeader);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        if (!resourceOwnerId.equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        return requestedVisibility;
    }

    public ResponseEntity<CursorResponse> getForUser(UUID id, Instant cursor, Pageable pageable) {
        int pageSize = pageable.getPageSize();
        Pageable limitPlusOne = PageRequest.of(0, pageSize + 1, Sort.by("createdAt").descending());

        List<BlogPageBase> base = blogRepository.findForMember(id, cursor, limitPlusOne);

        boolean hasNext = base.size() > pageSize;
        if (hasNext) {
            base = base.subList(0, pageSize);
        }

        Instant nextCursor = hasNext ? base.get(base.size() - 1).getCreatedAt() : null;

        List<BlogTagView> tags =
                blogRepository.findTagsForBlogs(base.stream().map(BlogPageBase::getId).toList());

        Map<UUID, List<String>> tagMap = PostServiceHelper.tagMapper.apply(tags);
        Map<UUID, List<BlogCollaboratorDto>> collaboratorMap = loadCollaborators(base);
        List<BlogPagesDto> pages = PostServiceHelper.pageMapper(base, tagMap, collaboratorMap);

        if (pages.isEmpty()) {
            return ResponseEntity.ok(new CursorResponse(List.of(), false, null));
        }

        return ResponseEntity.ok(new CursorResponse(pages, hasNext, nextCursor));
    }

    public ResponseEntity<RoleResponse> getRole(UUID userId, UUID blogId) {
        BlogMember role =
                blogRoleRepository
                        .findByBlogIdAndUserId(blogId, userId)
                        .orElseThrow(() -> new RoleNotFoundException("Unauthorized"));

        return ResponseEntity.ok(
                new RoleResponse(
                        true,
                        role.getRole().name(),
                        role.getRole() == Roles.READER ? true : false,
                        HttpStatus.OK,
                        "has reader access to this document"));
    }

    @Transactional
    public ResponseEntity<CommonResponse<String>> updateVisibility(
            UUID blogId, UUID userId, VisibilityUpdateRequest request) {
        BlogMember role =
                blogRoleRepository
                        .findByBlogIdAndUserId(blogId, userId)
                        .orElseThrow(() -> new RoleNotFoundException("Unauthorized"));

        if (role.getRole() != Roles.OWNER) {
            throw new RoleNotFoundException("Unauthorized");
        }

        Blog blog =
                blogRepository
                        .findById(blogId)
                        .orElseThrow(() -> new EntityNotFoundException("Blog does not found !"));

        if (request.visibility() != null) {
            blog.setVisibility(request.visibility());
        }
        if (request.status() != null) {
            blog.setStatus(request.status());
        }

        blogRepository.save(blog);
        return buildResponse(HttpStatus.OK, "ok");
    }

    private void assertCanEdit(UUID blogId, UUID userId) {
        BlogMember member =
                blogRoleRepository
                        .findByBlogIdAndUserId(blogId, userId)
                        .orElseThrow(() -> new RoleNotFoundException("Unauthorized"));

        if (member.getRole() != Roles.OWNER && member.getRole() != Roles.EDITOR) {
            throw new AccessDeniedException("No write access");
        }
    }

    private Map<UUID, List<BlogCollaboratorDto>> loadCollaborators(List<BlogPageBase> base) {
        List<UUID> blogIds = base.stream().map(BlogPageBase::getId).toList();
        if (blogIds.isEmpty()) {
            return Map.of();
        }

        return PostServiceHelper.collaboratorMapper(
                blogRoleRepository.findAllByBlogIdInAndRoleIn(
                        blogIds, List.of(Roles.OWNER, Roles.EDITOR)));
    }
}
