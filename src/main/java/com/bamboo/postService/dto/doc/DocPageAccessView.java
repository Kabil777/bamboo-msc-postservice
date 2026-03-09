package com.bamboo.postService.dto.doc;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Visibility;

import java.util.UUID;

public interface DocPageAccessView {

    UUID getDocId();

    String getContent();

    Visibility getVisibility();

    PostStatus getStatus();
}
