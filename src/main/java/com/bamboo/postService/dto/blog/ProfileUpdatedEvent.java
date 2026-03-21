package com.bamboo.postService.dto.blog;

import java.util.UUID;

public record ProfileUpdatedEvent(UUID id, String name, String handle, String coverUrl) {}
