package com.bamboo.postService.service;

import com.bamboo.postService.common.helper.PostServiceHelper;
import com.bamboo.postService.dto.comment.CommentCursorResponseDto;
import com.bamboo.postService.dto.comment.CommentReplyResponseDto;
import com.bamboo.postService.dto.comment.CommentResponseDto;
import com.bamboo.postService.dto.common.AuthorSummaryV1Dto;
import com.bamboo.postService.dto.feign.UserMetaDto;
import com.bamboo.postService.entity.AuthorProfileProjection;
import com.bamboo.postService.entity.Comment;
import com.bamboo.postService.feign.UserServiceClient;
import com.bamboo.postService.repository.CommentRepository;
import com.bamboo.postService.repository.AuthorProfileProjectionRepository;
import com.bamboo.postService.service.AuthorProjectionService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommentQueryService {

    private final CommentRepository commentRepository;
    private final AuthorProfileProjectionRepository authorProfileProjectionRepository;
    private final UserServiceClient userServiceClient;
    private final AuthorProjectionService authorProjectionService;

    public CommentQueryService(
            CommentRepository commentRepository,
            AuthorProfileProjectionRepository authorProfileProjectionRepository,
            UserServiceClient userServiceClient,
            AuthorProjectionService authorProjectionService) {
        this.commentRepository = commentRepository;
        this.authorProfileProjectionRepository = authorProfileProjectionRepository;
        this.userServiceClient = userServiceClient;
        this.authorProjectionService = authorProjectionService;
    }

    public CommentCursorResponseDto getCommentsByRoom(String room, Instant cursor, int limit) {
        limit = Math.max(1, limit);
        Pageable limitPlusOne = PageRequest.of(0, limit + 1, Sort.by("createdAt").descending());

        List<Comment> base =
                commentRepository.findByRoomAndCreatedAtLessThanOrderByCreatedAtDesc(
                        room, cursor, limitPlusOne);
        Map<UUID, AuthorSummaryV1Dto> authorMap = loadAuthors(base);

        List<CommentResponseDto> comments = base.stream().map(comment -> toResponseDto(comment, authorMap)).toList();

        boolean hasNext = comments.size() > limit;
        if (hasNext) {
            comments = comments.subList(0, limit);
        }

        Instant nextCursor = hasNext ? comments.get(comments.size() - 1).createdAt() : null;

        return comments.isEmpty()
                ? new CommentCursorResponseDto(List.of(), false, null)
                : new CommentCursorResponseDto(comments, hasNext, nextCursor);
    }

    private CommentResponseDto toResponseDto(
            Comment comment, Map<UUID, AuthorSummaryV1Dto> authorMap) {
        return new CommentResponseDto(
                comment.getId(),
                comment.getRoom(),
                comment.getContent(),
                resolveAuthor(comment.getUserId(), authorMap),
                mapReplies(comment, authorMap),
                comment.getCreatedAt());
    }

    private List<CommentReplyResponseDto> mapReplies(
            Comment comment, Map<UUID, AuthorSummaryV1Dto> authorMap) {
        if (comment.getReplies() == null || comment.getReplies().isEmpty()) {
            return List.of();
        }

        return comment.getReplies().stream()
                .map(
                        reply ->
                                new CommentReplyResponseDto(
                                        reply.id(),
                                        reply.content(),
                                        resolveAuthor(reply.userId(), authorMap)))
                .toList();
    }

    private Map<UUID, AuthorSummaryV1Dto> loadAuthors(List<Comment> comments) {
        Set<UUID> authorIds = new HashSet<>();
        for (Comment comment : comments) {
            authorIds.add(comment.getUserId());
            if (comment.getReplies() == null) {
                continue;
            }
            comment.getReplies().forEach(reply -> authorIds.add(reply.userId()));
        }

        if (authorIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, AuthorSummaryV1Dto> authorMap =
                authorProfileProjectionRepository.findAllById(authorIds).stream()
                        .filter(this::hasUsableAuthorSnapshot)
                        .collect(
                                Collectors.toMap(
                                        AuthorProfileProjection::getId,
                                        PostServiceHelper::authorMapper));

        return hydrateMissingAuthors(authorIds, authorMap);
    }

    private Map<UUID, AuthorSummaryV1Dto> hydrateMissingAuthors(
            Set<UUID> authorIds, Map<UUID, AuthorSummaryV1Dto> authorMap) {
        Set<UUID> missingAuthorIds = new HashSet<>(authorIds);
        missingAuthorIds.removeAll(authorMap.keySet());

        if (missingAuthorIds.isEmpty()) {
            return authorMap;
        }

        for (UUID missingAuthorId : missingAuthorIds) {
            UserMetaDto user = userServiceClient.getUserById(missingAuthorId);
            authorProjectionService.upsert(user);
        }

        return authorProfileProjectionRepository.findAllById(authorIds).stream()
                .collect(
                        Collectors.toMap(
                                AuthorProfileProjection::getId,
                                PostServiceHelper::authorMapper));
    }

    private AuthorSummaryV1Dto resolveAuthor(
            UUID userId, Map<UUID, AuthorSummaryV1Dto> authorMap) {
        AuthorSummaryV1Dto author = authorMap.get(userId);
        if (author != null && hasUsableAuthor(author)) {
            return author;
        }

        UserMetaDto user = userServiceClient.getUserById(userId);
        authorProjectionService.upsert(user);
        return PostServiceHelper.authorMapper(
                user.id(), user.name(), user.handle(), user.coverUrl());
    }

    private boolean hasUsableAuthorSnapshot(AuthorProfileProjection projection) {
        return isNotBlank(projection.getName()) && isNotBlank(projection.getHandle());
    }

    private boolean hasUsableAuthor(AuthorSummaryV1Dto author) {
        return isNotBlank(author.name()) && isNotBlank(author.handle());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
