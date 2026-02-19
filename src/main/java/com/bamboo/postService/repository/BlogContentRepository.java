package com.bamboo.postService.repository;

import com.bamboo.postService.entity.BlogContent;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BlogContentRepository extends JpaRepository<BlogContent, UUID> {

    @Modifying
    @Transactional
    @Query(
            value =
                    """
                        INSERT INTO blog_content (blog_id, content)
                        VALUES (:blogId, :content)
                        ON CONFLICT (blog_id)
                        DO UPDATE SET
                            content = EXCLUDED.content
                    """,
            nativeQuery = true)
    void upsertContent(@Param("blogId") UUID blogId, @Param("content") String content);
}
