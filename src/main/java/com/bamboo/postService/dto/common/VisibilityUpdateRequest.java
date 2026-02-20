package com.bamboo.postService.dto.common;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Visibility;

public record VisibilityUpdateRequest(Visibility visibility, PostStatus status) {}
