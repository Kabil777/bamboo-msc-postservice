package com.bamboo.postService.common.response;

import java.util.UUID;

public record DocsMemberRoleResponse(
        UUID userId,
        String name,
        String handle,
        String coverUrl,
        String email,
        String role) {}
