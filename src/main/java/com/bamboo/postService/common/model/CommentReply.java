package com.bamboo.postService.common.model;

import java.util.UUID;

public record CommentReply(UUID id, String content, UUID userId) {}
