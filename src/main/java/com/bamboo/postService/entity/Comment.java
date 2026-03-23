package com.bamboo.postService.entity;

import com.bamboo.postService.common.model.CommentReply;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "comment",
        indexes = {
            @Index(name = "idx_comment_room_created_at", columnList = "room, createdAt"),
            @Index(name = "idx_comment_user_id", columnList = "userId")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String room;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = true, columnDefinition = "json")
    private List<CommentReply> replies;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;
}
