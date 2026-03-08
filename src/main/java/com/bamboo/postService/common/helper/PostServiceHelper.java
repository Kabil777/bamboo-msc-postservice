package com.bamboo.postService.common.helper;

import com.bamboo.postService.dto.blog.BlogPageBase;
import com.bamboo.postService.dto.blog.BlogCollaboratorDto;
import com.bamboo.postService.dto.blog.BlogPagesDto;
import com.bamboo.postService.dto.blog.BlogTagView;
import com.bamboo.postService.entity.BlogMember;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    public static Map<UUID, List<BlogCollaboratorDto>> collaboratorMapper(List<BlogMember> members) {
        return members.stream()
                .collect(
                        Collectors.groupingBy(
                                BlogMember::getBlogId,
                                Collectors.mapping(
                                        member ->
                                                new BlogCollaboratorDto(
                                                        member.getUserId(),
                                                        member.getUserName(),
                                                        member.getUserHandle(),
                                                        member.getUserCoverUrl()),
                                        Collectors.toList())));
    }

    public static List<BlogPagesDto> pageMapper(
            List<BlogPageBase> base,
            Map<UUID, List<String>> tag,
            Map<UUID, List<BlogCollaboratorDto>> collaborators) {
        return base.stream()
                .map(
                        b ->
                                new BlogPagesDto(
                                        tag.getOrDefault(b.getId(), Collections.emptyList()),
                                        b.getId(),
                                        b.getTitle(),
                                        b.getCoverUrl(),
                                        b.getDescription(),
                                        b.getAuthorId(),
                                        b.getAuthorName(),
                                        b.getAuthorHandle(),
                                        b.getAuthorAvatar(),
                                        b.getCreatedAt(),
                                        b.getVisibility(),
                                        b.getStatus(),
                                        collaborators.getOrDefault(
                                                b.getId(), Collections.emptyList())))
                .toList();
    }
}
