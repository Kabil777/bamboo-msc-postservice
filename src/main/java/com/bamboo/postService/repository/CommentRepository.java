package com.bamboo.postService.repository;

import com.bamboo.postService.entity.Comment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByRoomAndCreatedAtLessThanOrderByCreatedAtDesc(
            String room, Instant cursor, Pageable pageable);
}
