package com.bamboo.postService.dto.blog;

import java.util.UUID;

public record BlogCollaboratorDto(
        UUID id, String name, String handle, String avatarUrl) {}
