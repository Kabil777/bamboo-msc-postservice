package com.bamboo.postService.dto.comment;

import com.bamboo.postService.common.enums.IncomingCommentReqType;

import java.util.UUID;

public record IncomingCommentDeleteReq(
        IncomingCommentReqType type,
        String room,
        UUID userId,
        UUID commentId,
        Boolean isReply,
        UUID replyId) {}
