package com.bamboo.postService.dto.comment;

import com.bamboo.postService.common.enums.IncomingCommentReqType;

import java.util.UUID;

public record IncomingCommentReq(
        IncomingCommentReqType type,
        String room,
        UUID userId,
        String content,
        Boolean isReply,
        UUID replyId) {}
