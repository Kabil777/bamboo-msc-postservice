package com.bamboo.postService.dto.doc;

import com.bamboo.postService.common.model.PageNode;

import java.util.List;

public record DocsContentRequest(List<PageNode> tree, List<DocPageContentRequest> pages) {}
