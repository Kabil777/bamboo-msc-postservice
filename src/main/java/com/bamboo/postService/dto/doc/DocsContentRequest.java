package com.bamboo.postService.dto.doc;

import com.bamboo.postService.common.model.PageNode;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.common.enums.PostStatus;

import java.util.List;

public record DocsContentRequest(
        List<PageNode> tree,
        List<DocPageContentRequest> pages,
        Visibility visibility,
        PostStatus status) {}
