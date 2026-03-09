package com.bamboo.postService.dto.common;

import java.util.UUID;

public record AuthorSummaryV1Dto(UUID id, String name, String handle, String avatarUrl) {}
