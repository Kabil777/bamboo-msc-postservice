package com.bamboo.postService.dto.blog;

import java.time.Instant;
import java.util.UUID;

public interface BlogPageBase {

    UUID getId();

    String getTitle();

    String getCoverUrl();

    String getDescription();

    Instant getCreatedAt();

    UUID getAuthorId();

    String getAuthorName();

    String getAuthorHandle();

    String getAuthorAvatar();
}
