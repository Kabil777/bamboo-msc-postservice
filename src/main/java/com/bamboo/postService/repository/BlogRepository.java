package com.bamboo.postService.repository;

import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.dto.blog.BlogPageBase;
import com.bamboo.postService.dto.blog.BlogTagView;
import com.bamboo.postService.entity.Blog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlogRepository extends JpaRepository<Blog, UUID>, JpaSpecificationExecutor<Blog> {

    @Query(
            """
                    SELECT b.id AS id,
                       b.title AS title,
                       b.coverUrl AS coverUrl,
                       b.description AS description,
                       b.createdAt AS createdAt,
            b.authorSnapshot.id AS authorId
                FROM Blog b
                WHERE b.visibility = :visibility
                order by b.createdAt desc
            """)
    Page<BlogPageBase> findPublicBlogs(
            @Param("visibility") Visibility visibility, Pageable pageable);

    @Query(
"""
    SELECT
        b.id AS id,
        b.title AS title,
        b.coverUrl AS coverUrl,
        b.description AS description,
        b.createdAt AS createdAt,
        b.authorSnapshot.id AS authorId,
        b.authorSnapshot.name AS authorName,
        b.authorSnapshot.handle AS authorHandle,
        b.authorSnapshot.avatarUrl AS authorAvatar
    FROM Blog b
    WHERE b.authorSnapshot.id = :authorId
      AND b.visibility = :visibility
      AND b.createdAt < :cursor
    ORDER BY b.createdAt DESC
""")
    List<BlogPageBase> findByAuthorId(
            @Param("authorId") UUID authorId,
            @Param("visibility") Visibility visibility,
            @Param("cursor") Instant cursor,
            Pageable pageable);

    @Query(
"""
    SELECT
        b.id AS id,
        b.title AS title,
        b.coverUrl AS coverUrl,
        b.description AS description,
        b.createdAt AS createdAt,
        b.authorSnapshot.id AS authorId,
        b.authorSnapshot.handle AS authorHandle
    FROM Blog b
    WHERE b.authorSnapshot.id = :authorId
      AND b.createdAt < :cursor
    ORDER BY b.createdAt DESC
""")
    List<BlogPageBase> findForAuthor(
            @Param("authorId") UUID authorId, @Param("cursor") Instant cursor, Pageable pageable);

    @Query(
            """
                     SELECT b.id AS id,
                        b.title AS title,
                        b.coverUrl AS coverUrl,
                        b.description AS description,
                        b.createdAt AS createdAt,
            b.authorSnapshot.id AS authorId
                     FROM Blog b
                     WHERE b.visibility = :visibility
                     AND  b.createdAt < :cursor
                     AND EXISTS (
                        SELECT 1 FROM b.tags t
                        WHERE t.tag = :tag
                   )
                   ORDER BY b.createdAt DESC
            """)
    List<BlogPageBase> findByTags(
            @Param("visibility") Visibility visibility,
            @Param("tag") String tag,
            @Param("cursor") Instant cursor,
            Pageable pageable);

    @Query(
            """
                select b.id AS id,
                        b.title AS title,
                        b.coverUrl AS coverUrl,
                        b.description AS description,
                        b.createdAt AS createdAt,
            b.authorSnapshot.id AS authorId
                FROM Blog b
                WHERE b.visibility = :visibility
                AND b.createdAt < :cursor
                Order by b.createdAt DESC
            """)
    List<BlogPageBase> findNextBlogPageBases(
            @Param("visibility") Visibility visibility,
            @Param("cursor") Instant cursor,
            Pageable pageable);

    @Query(
            """
            select b.id as id,t.tag as tag
                from Blog b
                join b.tags t
                Where b.id in :blogIds
            """)
    List<BlogTagView> findTagsForBlogs(@Param("blogIds") List<UUID> ids);

    @Query(
            """
            select t.tag as tag
                from Blog b
                join b.tags t
                where b.id = :blogId
            """)
    List<String> findTagNamesByBlogId(@Param("blogId") UUID blogId);

    @Query(
"""
    SELECT DISTINCT b
    FROM Blog b
    LEFT JOIN FETCH b.content
    WHERE b.id = :id
""")
    Optional<Blog> findBlogWithContent(@Param("id") UUID id);
}
