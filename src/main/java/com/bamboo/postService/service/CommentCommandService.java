package com.bamboo.postService.service;

import com.bamboo.postService.common.model.CommentReply;
import com.bamboo.postService.dto.comment.IncomingCommentDeleteReq;
import com.bamboo.postService.dto.comment.IncomingCommentReq;
import com.bamboo.postService.dto.feign.UserMetaDto;
import com.bamboo.postService.entity.Comment;
import com.bamboo.postService.feign.UserServiceClient;
import com.bamboo.postService.repository.AuthorProfileProjectionRepository;
import com.bamboo.postService.repository.CommentRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CommentCommandService {

    private final CommentRepository commentRepository;
    private final AuthorProfileProjectionRepository authorProfileProjectionRepository;
    private final AuthorProjectionService authorProjectionService;
    private final UserServiceClient userServiceClient;

    public CommentCommandService(
            CommentRepository commentRepository,
            AuthorProfileProjectionRepository authorProfileProjectionRepository,
            AuthorProjectionService authorProjectionService,
            UserServiceClient userServiceClient) {
        this.commentRepository = commentRepository;
        this.authorProfileProjectionRepository = authorProfileProjectionRepository;
        this.authorProjectionService = authorProjectionService;
        this.userServiceClient = userServiceClient;
    }

    @Transactional
    public void createComment(IncomingCommentReq request) {
        ensureAuthorProjection(request.userId());

        if (Boolean.TRUE.equals(request.isReply())) {
            UUID parentCommentId = request.replyId();
            if (parentCommentId == null) {
                throw new IllegalArgumentException("replyId is required for reply comments");
            }

            Comment parentComment =
                    commentRepository
                            .findById(parentCommentId)
                            .orElseThrow(
                                    () -> new EntityNotFoundException("Parent comment not found"));

            List<CommentReply> replies =
                    new ArrayList<>(
                            parentComment.getReplies() == null
                                    ? List.of()
                                    : parentComment.getReplies());
            replies.add(new CommentReply(UUID.randomUUID(), request.content(), request.userId()));
            parentComment.setReplies(replies);
            commentRepository.save(parentComment);
            return;
        }

        commentRepository.save(
                Comment.builder()
                        .room(request.room())
                        .userId(request.userId())
                        .content(request.content())
                        .replies(new ArrayList<>())
                        .build());
    }

    @Transactional
    public void deleteComment(IncomingCommentDeleteReq request) {
        if (Boolean.TRUE.equals(request.isReply())) {
            UUID parentCommentId = request.commentId();
            UUID replyId = request.replyId();
            if (parentCommentId == null || replyId == null) {
                throw new IllegalArgumentException(
                        "commentId and replyId are required for reply delete");
            }

            Comment parentComment = commentRepository.findById(parentCommentId).orElse(null);
            if (parentComment == null || !request.room().equals(parentComment.getRoom())) {
                return;
            }

            List<CommentReply> replies =
                    new ArrayList<>(
                            parentComment.getReplies() == null
                                    ? List.of()
                                    : parentComment.getReplies());
            CommentReply replyToDelete =
                    replies.stream()
                            .filter(reply -> reply.id().equals(replyId))
                            .findFirst()
                            .orElse(null);
            if (replyToDelete == null) {
                return;
            }

            if (!replyToDelete.userId().equals(request.userId())) {
                return;
            }

            replies.removeIf(reply -> reply.id().equals(replyId));
            parentComment.setReplies(replies);
            commentRepository.save(parentComment);
            return;
        }

        Comment comment = commentRepository.findById(request.commentId()).orElse(null);
        if (comment == null || !request.room().equals(comment.getRoom())) {
            return;
        }

        if (!comment.getUserId().equals(request.userId())) {
            return;
        }

        commentRepository.delete(comment);
    }

    private void ensureAuthorProjection(UUID userId) {
        authorProfileProjectionRepository
                .findById(userId)
                .filter(this::hasUsableAuthorSnapshot)
                .orElseGet(
                        () -> {
                            UserMetaDto user = userServiceClient.getUserById(userId);
                            return authorProjectionService.upsert(user);
                        });
    }

    private boolean hasUsableAuthorSnapshot(
            com.bamboo.postService.entity.AuthorProfileProjection projection) {
        return isNotBlank(projection.getName()) && isNotBlank(projection.getHandle());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
