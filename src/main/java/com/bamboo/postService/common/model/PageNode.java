package com.bamboo.postService.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageNode {
    UUID id;
    String title;
    String content;

    List<PageNode> subTree;
}
