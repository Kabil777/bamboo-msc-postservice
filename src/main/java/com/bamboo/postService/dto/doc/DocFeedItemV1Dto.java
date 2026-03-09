package com.bamboo.postService.dto.doc;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.dto.common.AuthorSummaryV1Dto;

import java.time.Instant;
import java.util.UUID;

public record DocFeedItemV1Dto(
        UUID id,
        String title,
        String coverUrl,
        String description,
        Instant createdAt,
        Visibility visibility,
        PostStatus status,
        AuthorSummaryV1Dto author) {}
