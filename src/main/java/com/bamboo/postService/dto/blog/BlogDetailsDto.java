package com.bamboo.postService.dto.blog;

import com.bamboo.postService.entity.AuthorSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BlogDetailsDto(
        UUID id,
        String title,
        String description,
        String coverUrl,
        String content,
        Instant createdAt,
        List<String> tags,
        AuthorSnapshot authorSnapshot) {}
