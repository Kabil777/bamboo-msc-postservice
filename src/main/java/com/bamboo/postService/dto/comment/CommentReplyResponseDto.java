package com.bamboo.postService.dto.comment;

import com.bamboo.postService.dto.common.AuthorSummaryV1Dto;

import java.util.UUID;

public record CommentReplyResponseDto(UUID id, String content, AuthorSummaryV1Dto author) {}
