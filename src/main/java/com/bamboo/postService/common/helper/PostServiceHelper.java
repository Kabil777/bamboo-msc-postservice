package com.bamboo.postService.common.helper;

import com.bamboo.postService.dto.blog.BlogPageBase;
import com.bamboo.postService.dto.blog.BlogFeedItemV1Dto;
import com.bamboo.postService.dto.blog.BlogTagView;
import com.bamboo.postService.dto.common.AuthorSummaryV1Dto;
import com.bamboo.postService.entity.AuthorSnapshot;
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

    public static AuthorSummaryV1Dto authorMapper(AuthorSnapshot snapshot) {
        return new AuthorSummaryV1Dto(
                snapshot.getId(), snapshot.getName(), snapshot.getHandle(), snapshot.getAvatarUrl());
    }

    public static AuthorSummaryV1Dto authorMapper(
            UUID id, String name, String handle, String avatarUrl) {
        return new AuthorSummaryV1Dto(id, name, handle, avatarUrl);
    }

    public static Map<UUID, List<AuthorSummaryV1Dto>> collaboratorMapper(List<BlogMember> members) {
        return members.stream()
                .collect(
                        Collectors.groupingBy(
                                BlogMember::getBlogId,
                                Collectors.mapping(
                                        member ->
                                                new AuthorSummaryV1Dto(
                                                        member.getUserId(),
                                                        member.getUserName(),
                                                        member.getUserHandle(),
                                                        member.getUserCoverUrl()),
                                        Collectors.toList())));
    }

    public static List<BlogFeedItemV1Dto> pageMapper(
            List<BlogPageBase> base,
            Map<UUID, List<String>> tag,
            Map<UUID, List<AuthorSummaryV1Dto>> collaborators) {
        return base.stream()
                .map(
                        b ->
                                new BlogFeedItemV1Dto(
                                        b.getId(),
                                        b.getTitle(),
                                        b.getCoverUrl(),
                                        b.getDescription(),
                                        tag.getOrDefault(b.getId(), Collections.emptyList()),
                                        b.getCreatedAt(),
                                        b.getVisibility(),
                                        b.getStatus(),
                                        authorMapper(
                                                b.getAuthorId(),
                                                b.getAuthorName(),
                                                b.getAuthorHandle(),
                                                b.getAuthorAvatar()),
                                        collaborators.getOrDefault(
                                                b.getId(), Collections.emptyList())))
                .toList();
    }
}
