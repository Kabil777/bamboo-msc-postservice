package com.bamboo.postService.dto.collab;

import java.util.UUID;

public record CollabDeleteEvent(String type, UUID id) {}
