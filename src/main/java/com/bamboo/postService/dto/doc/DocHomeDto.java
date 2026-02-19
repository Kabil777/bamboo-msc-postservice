package com.bamboo.postService.dto.doc;

import java.time.Instant;
import java.util.UUID;

public record DocHomeDto(
        UUID id, String title, String coverUrl, String description, Instant createdAt) {}
