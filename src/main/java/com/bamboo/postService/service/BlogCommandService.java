package com.bamboo.postService.service;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Roles;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.dto.blog.MetaPostDto;
import com.bamboo.postService.dto.common.VisibilityUpdateRequest;
import com.bamboo.postService.dto.feign.UserMetaDto;
import com.bamboo.postService.entity.AuthorProfileProjection;
import com.bamboo.postService.entity.Blog;
import com.bamboo.postService.entity.BlogMember;
import com.bamboo.postService.entity.Tags;
import com.bamboo.postService.exception.UserNotFoundException;
import com.bamboo.postService.feign.UserServiceClient;
import com.bamboo.postService.policy.PostAccessPolicy;
import com.bamboo.postService.repository.BlogContentRepository;
import com.bamboo.postService.repository.BlogRepository;
import com.bamboo.postService.repository.BlogRoleRepository;
import com.bamboo.postService.repository.TagRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class BlogCommandService {

    private final BlogRepository blogRepository;
    private final TagRepository tagRepository;
    private final BlogContentRepository blogContentRepository;
    private final BlogRoleRepository blogRoleRepository;
    private final UserServiceClient userServiceClient;
    private final PostAccessPolicy postAccessPolicy;
    private final AuthorProjectionService authorProjectionService;

    public BlogCommandService(
            BlogRepository blogRepository,
            TagRepository tagRepository,
            BlogContentRepository blogContentRepository,
            BlogRoleRepository blogRoleRepository,
            UserServiceClient userServiceClient,
            PostAccessPolicy postAccessPolicy,
            AuthorProjectionService authorProjectionService) {
        this.blogRepository = blogRepository;
        this.tagRepository = tagRepository;
        this.blogContentRepository = blogContentRepository;
        this.blogRoleRepository = blogRoleRepository;
        this.userServiceClient = userServiceClient;
        this.postAccessPolicy = postAccessPolicy;
        this.authorProjectionService = authorProjectionService;
    }

    @Transactional
    public String saveContent(
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
        return "ok";
    }

    @Transactional
    public Map<String, UUID> save(MetaPostDto blogDto, UUID userId) {
        UserMetaDto actor = resolveUserById(userId);
        AuthorProfileProjection authorProfile = authorProjectionService.upsert(actor);

        Blog blog =
                Blog.builder()
                        .title(blogDto.title())
                        .coverUrl(blogDto.coverUrl())
                        .description(blogDto.description())
                        .authorProfile(authorProfile)
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
                        .userId(authorProfile.getId())
                        .userName(authorProfile.getName())
                        .userHandle(authorProfile.getHandle())
                        .userCoverUrl(authorProfile.getAvatarUrl())
                        .userEmail(actor.email())
                        .role(Roles.OWNER)
                        .build());
        return Map.of("id", blog.getId());
    }

    @Transactional
    public String updateVisibility(UUID blogId, UUID userId, VisibilityUpdateRequest request) {
        postAccessPolicy.assertCanManage(
                resolveBlogRole(blogId, userId), "Only owner can manage blog visibility");

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
        return "ok";
    }

    @Transactional
    public void deleteById(UUID blogId, UUID userId) {
        postAccessPolicy.assertCanManage(
                resolveBlogRole(blogId, userId), "Only owner can delete blog");

        Blog blog =
                blogRepository
                        .findById(blogId)
                        .orElseThrow(() -> new EntityNotFoundException("Blog does not found !"));

        blogRoleRepository.deleteAllByBlogId(blogId);
        blogRepository.delete(blog);
    }

    private void assertCanEdit(UUID blogId, UUID userId) {
        postAccessPolicy.assertCanEdit(resolveBlogRole(blogId, userId));
    }

    private Roles resolveBlogRole(UUID blogId, UUID userId) {
        if (userId == null) {
            return null;
        }

        return blogRoleRepository
                .findByBlogIdAndUserId(blogId, userId)
                .map(BlogMember::getRole)
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
