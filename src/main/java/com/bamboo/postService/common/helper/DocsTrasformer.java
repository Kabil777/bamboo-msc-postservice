package com.bamboo.postService.common.helper;

import com.bamboo.postService.common.model.PageNode;
import com.bamboo.postService.dto.doc.DocPageRequestNode;
import com.bamboo.postService.entity.Pages;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DocsTrasformer {
    public static TransformResult transform(UUID docId, List<DocPageRequestNode> pageNodes) {
        List<PageNode> tree = new ArrayList<>();
        List<Pages> pages = new ArrayList<>();

        for (DocPageRequestNode node : pageNodes) {
            tree.add(buildNode(docId, node, pages));
        }

        return new TransformResult(tree, pages);
    }

    public static PageNode buildNode(UUID docId, DocPageRequestNode node, List<Pages> pages) {
        UUID pageId = UUID.randomUUID();

        PageNode pageNode =
                new PageNode(pageId, node.getTitle(), node.getContent(), new ArrayList<>());

        Pages page = new Pages(pageId, docId, node.getContent());
        pages.add(page);

        if (node.subPages != null) {
            for (DocPageRequestNode subNode : node.getSubPages()) {
                pageNode.getSubTree().add(buildNode(docId, subNode, pages));
            }
        }

        return pageNode;
    }

    public record TransformResult(List<PageNode> tree, List<Pages> pages) {}
}
