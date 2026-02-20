package com.bamboo.postService.dto.blog;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Visibility;

public record BlogPageDto(String content, Visibility visibility, PostStatus status) {}
