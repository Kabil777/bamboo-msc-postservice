package com.bamboo.postService.service;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Roles;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.dto.blog.MetaPostDto;
import com.bamboo.postService.dto.blog.ProfileUpdatedEvent;
import com.bamboo.postService.dto.collab.CollabDeleteEvent;
import com.bamboo.postService.dto.common.VisibilityUpdateRequest;
import com.bamboo.postService.dto.count.ContentCountEvent;
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

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final RabbitTemplate rabbitTemplate;

    public BlogCommandService(
            BlogRepository blogRepository,
            TagRepository tagRepository,
            BlogContentRepository blogContentRepository,
            BlogRoleRepository blogRoleRepository,
            UserServiceClient userServiceClient,
            PostAccessPolicy postAccessPolicy,
            AuthorProjectionService authorProjectionService,
            RabbitTemplate rabbitTemplate) {
        this.blogRepository = blogRepository;
        this.tagRepository = tagRepository;
        this.blogContentRepository = blogContentRepository;
        this.blogRoleRepository = blogRoleRepository;
        this.userServiceClient = userServiceClient;
        this.postAccessPolicy = postAccessPolicy;
        this.authorProjectionService = authorProjectionService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public String saveContent(
            UUID userId, UUID blogId, String content, Visibility visibility, PostStatus status) {
        assertCanEdit(blogId, userId);

        if ((visibility != null || status != null) && !StringUtils.hasText(content)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Content is required before visibility/status update");
        }

        blogContentRepository.upsertContent(blogId, content);
        Blog blog =
                blogRepository
                        .findById(blogId)
                        .orElseThrow(() -> new EntityNotFoundException("Blog does not found !"));
        Visibility previousVisibility = blog.getVisibility();
        if (visibility != null) {
            blog.setVisibility(visibility);
        }
        if (status != null) {
            blog.setStatus(status);
        } else if (visibility != null) {
            blog.setStatus(PostStatus.PUBLISHED);
        }
        blogRepository.save(blog);
        publishContentCountEvent(
                blog.getAuthorProfile().getId(), "BLOG", previousVisibility, blog.getVisibility());
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
        publishContentCountCreate(authorProfile.getId(), "BLOG", blog.getVisibility());
        return Map.of("id", blog.getId());
    }

    @Transactional
    public String updateVisibility(UUID blogId, UUID userId, VisibilityUpdateRequest request) {
        postAccessPolicy.assertCanManage(
                resolveBlogRole(blogId, userId), "Only owner can manage blog visibility");

        String content = blogContentRepository.findContentByBlogId(blogId).orElse(null);
        if (!StringUtils.hasText(content)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot change visibility for empty content");
        }

        Blog blog =
                blogRepository
                        .findById(blogId)
                        .orElseThrow(() -> new EntityNotFoundException("Blog does not found !"));
        Visibility previousVisibility = blog.getVisibility();

        if (request.visibility() != null) {
            blog.setVisibility(request.visibility());
        }
        if (request.status() != null) {
            blog.setStatus(request.status());
        }

        blogRepository.save(blog);
        publishContentCountEvent(
                blog.getAuthorProfile().getId(), "BLOG", previousVisibility, blog.getVisibility());
        return "ok";
    }

    @Transactional
    public void syncAuthorProfile(ProfileUpdatedEvent event) {
        authorProjectionService.syncProfile(event);
    }

    @Transactional
    public void deleteById(UUID blogId, UUID userId) {
        postAccessPolicy.assertCanManage(
                resolveBlogRole(blogId, userId), "Only owner can delete blog");

        Blog blog =
                blogRepository
                        .findById(blogId)
                        .orElseThrow(() -> new EntityNotFoundException("Blog does not found !"));
        UUID authorId = blog.getAuthorProfile().getId();
        Visibility visibility = blog.getVisibility();

        blogRoleRepository.deleteAllByBlogId(blogId);
        blogRepository.delete(blog);
        publishContentCountDelete(authorId, "BLOG", visibility);
        publishCollabDeleteEvent("BLOG", blogId);
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
