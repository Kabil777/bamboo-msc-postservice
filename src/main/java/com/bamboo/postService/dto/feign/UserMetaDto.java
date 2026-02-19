package com.bamboo.postService.dto.feign;

import java.util.UUID;

public record UserMetaDto(UUID id, String name, String handle, String coverUrl, String email) {}
