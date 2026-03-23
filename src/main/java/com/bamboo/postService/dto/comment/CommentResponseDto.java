package com.bamboo.postService.dto.comment;

import com.bamboo.postService.dto.common.AuthorSummaryV1Dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentResponseDto(
        UUID id,
        String room,
        String content,
        AuthorSummaryV1Dto author,
        List<CommentReplyResponseDto> replies,
        Instant createdAt) {}
