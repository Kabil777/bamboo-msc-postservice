package com.bamboo.postService.dto.doc;

import lombok.Data;

import java.util.List;

@Data
public class DocPageRequestNode {
    public String title;
    public String content;

    public List<DocPageRequestNode> subPages;
}
