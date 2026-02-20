package com.bamboo.postService.repository;

import com.bamboo.postService.dto.blog.BlogTagView;
import com.bamboo.postService.dto.doc.DocHomeDto;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.entity.Docs;

import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DocsRepository extends JpaRepository<Docs, UUID> {

    @Query(
            """
            select d.id as id,t.tag as tag
                from Docs d
                join d.tags t
                Where d.id in :documnetIds
            """)
    List<BlogTagView> findTagsForDocuments(@Param("documentIds") List<UUID> ids);

    @Query(
            """
                select
                     d.id as id ,
                     d.title as title,
                     d.coverUrl as coverUrl ,
                     d.description as description ,
                     d.createdAt as createdAt,
                     d.visibility as visibility,
                     d.status as status
                    from Docs d
                    where d.visibility = :visibility
                    order by d.createdAt desc
            """)
    List<DocHomeDto> findAllCoverDocs(
            @Param("visibility") Visibility visibility, Pageable pageable);

    @Query(
            """
                select
                     d.id as id ,
                     d.title as title,
                     d.coverUrl as coverUrl ,
                     d.description as description ,
                     d.createdAt as createdAt,
                     d.visibility as visibility,
                     d.status as status
                    from Docs d
                    where d.authorSnapshot.id = :authorId
                      and d.createdAt < :cursor
                    order by d.createdAt desc
            """)
    List<DocHomeDto> findForAuthor(
            @Param("authorId") UUID authorId,
            @Param("cursor") java.time.Instant cursor,
            Pageable pageable);

    @Query(
            """
                select
                     d.id as id ,
                     d.title as title,
                     d.coverUrl as coverUrl ,
                     d.description as description ,
                     d.createdAt as createdAt,
                     d.visibility as visibility,
                     d.status as status
                    from Docs d
                    where d.authorSnapshot.id = :authorId
                      and d.visibility = :visibility
                      and d.createdAt < :cursor
                    order by d.createdAt desc
            """)
    List<DocHomeDto> findForAuthorWithVisibility(
            @Param("authorId") UUID authorId,
            @Param("visibility") Visibility visibility,
            @Param("cursor") java.time.Instant cursor,
            Pageable pageable);
}
