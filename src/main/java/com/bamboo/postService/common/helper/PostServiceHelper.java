package com.bamboo.postService.common.helper;

import com.bamboo.postService.dto.blog.BlogPageBase;
import com.bamboo.postService.dto.blog.BlogPagesDto;
import com.bamboo.postService.dto.blog.BlogTagView;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PostServiceHelper {

    private PostServiceHelper() {}

    public static Function<List<BlogTagView>, Map<UUID, List<String>>> tagMapper =
            (tags) -> {
                return tags.stream()
                        .collect(
                                Collectors.groupingBy(
                                        BlogTagView::getId,
                                        Collectors.mapping(
                                                BlogTagView::getTag, Collectors.toList())));
            };

    public static BiFunction<List<BlogPageBase>, Map<UUID, List<String>>, List<BlogPagesDto>>
            pageMapper =
                    (base, tag) -> {
                        return base.stream()
                                .map(
                                        b ->
                                                new BlogPagesDto(
                                                        tag.getOrDefault(
                                                                b.getId(), Collections.emptyList()),
                                                        b.getId(),
                                                        b.getTitle(),
                                                        b.getCoverUrl(),
                                                        b.getDescription(),
                                                        b.getAuthorId(),
                                                        b.getAuthorHandle(),
                                                        b.getCreatedAt(),
                                                        b.getVisibility(),
                                                        b.getStatus()))
                                .toList();
                    };
}
