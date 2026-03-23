package com.bamboo.postService.controller;

import com.bamboo.postService.dto.comment.CommentCursorResponseDto;
import com.bamboo.postService.dto.comment.IncomingCommentDeleteReq;
import com.bamboo.postService.dto.comment.IncomingCommentReq;
import com.bamboo.postService.service.CommentCommandService;
import com.bamboo.postService.service.CommentQueryService;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentCommandService commentCommandService;
    private final CommentQueryService commentQueryService;

    public CommentController(
            CommentCommandService commentCommandService,
            CommentQueryService commentQueryService) {
        this.commentCommandService = commentCommandService;
        this.commentQueryService = commentQueryService;
    }

    @GetMapping
    public ResponseEntity<CommentCursorResponseDto> getCommentsByRoom(
            @RequestParam String room,
            @RequestParam(required = false) Instant cursor,
            @RequestParam(defaultValue = "10") int limit) {
        if (cursor == null) cursor = Instant.now();

        CommentCursorResponseDto comments =
                commentQueryService.getCommentsByRoom(room, cursor, limit);
        if (comments.items().isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(comments);
    }

    @RabbitListener(queues = "queue.comment.published")
    public void createComment(IncomingCommentReq request) {
        commentCommandService.createComment(request);
    }

    @RabbitListener(queues = "queue.comment.deleted")
    public void deleteComment(IncomingCommentDeleteReq request) {
        commentCommandService.deleteComment(request);
    }
}
