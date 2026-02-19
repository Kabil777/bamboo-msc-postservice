package com.bamboo.postService.dto.blog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BlogPagesDto(
        List<String> tags,
        UUID id,
        String title,
        String coverUrl,
        String description,
        UUID authorId,
        String handle,
        Instant createdAt) {}
