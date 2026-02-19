package com.bamboo.postService.common.helper;

import com.bamboo.postService.common.model.PageNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocsTree {

    private List<PageNode> nodes;
}
