package com.bamboo.postService.dto.blog;

import java.time.Instant;
import java.util.UUID;

public record BlogPageContent(
        UUID id,
        String title,
        String coverUrl,
        String description,
        Instant createdAt,
        String content,
        UUID authorId) {}
