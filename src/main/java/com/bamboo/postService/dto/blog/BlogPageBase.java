package com.bamboo.postService.dto.blog;

import java.time.Instant;
import java.util.UUID;
import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Visibility;

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

    Visibility getVisibility();

    PostStatus getStatus();
}
