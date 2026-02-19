package com.bamboo.postService.common.response;

import java.util.UUID;

public record BlogMemberRoleResponse(
        UUID userId,
        String userName,
        String userHandle,
        String userCoverUrl,
        String userEmail,
        String role) {}
