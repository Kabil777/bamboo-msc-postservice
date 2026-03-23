package com.bamboo.postService.dto.comment;

import java.time.Instant;
import java.util.List;

public record CommentCursorResponseDto(
        List<CommentResponseDto> items, Boolean hasNext, Instant cursor) {}
