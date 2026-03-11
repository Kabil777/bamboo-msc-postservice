package com.bamboo.postService.common.helper;

import com.bamboo.postService.common.model.PageNode;
import com.bamboo.postService.dto.doc.DocPageRequestNode;
import com.bamboo.postService.entity.Pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        PageNode pageNode = new PageNode(pageId, node.getTitle(), "", new ArrayList<>());

        Pages page = new Pages(pageId, docId, node.getContent());
        pages.add(page);

        if (node.subPages != null) {
            for (DocPageRequestNode subNode : node.getSubPages()) {
                pageNode.getSubTree().add(buildNode(docId, subNode, pages));
            }
        }

        return pageNode;
    }

    public static List<PageNode> stripTreeContent(List<PageNode> tree) {
        if (tree == null) {
            return List.of();
        }

        return tree.stream().map(DocsTrasformer::stripNodeContent).toList();
    }

    public static List<PageNode> hydrateTreeContent(List<PageNode> tree, List<Pages> pages) {
        if (tree == null) {
            return List.of();
        }

        Map<UUID, String> contentByPageId =
                pages == null
                        ? Collections.emptyMap()
                        : pages.stream()
                                .collect(
                                        Collectors.toMap(
                                                Pages::getPageId,
                                                page -> page.getContent() == null ? "" : page.getContent(),
                                                (left, right) -> right));

        return tree.stream().map(node -> hydrateNodeContent(node, contentByPageId)).toList();
    }

    private static PageNode stripNodeContent(PageNode node) {
        return new PageNode(
                node.getId(),
                node.getTitle(),
                "",
                mapSubTree(node, DocsTrasformer::stripNodeContent));
    }

    private static PageNode hydrateNodeContent(PageNode node, Map<UUID, String> contentByPageId) {
        return new PageNode(
                node.getId(),
                node.getTitle(),
                contentByPageId.getOrDefault(node.getId(), ""),
                mapSubTree(node, child -> hydrateNodeContent(child, contentByPageId)));
    }

    private static List<PageNode> mapSubTree(
            PageNode node, Function<PageNode, PageNode> mapper) {
        if (node.getSubTree() == null || node.getSubTree().isEmpty()) {
            return List.of();
        }

        return node.getSubTree().stream().map(mapper).toList();
    }

    public record TransformResult(List<PageNode> tree, List<Pages> pages) {}
}
