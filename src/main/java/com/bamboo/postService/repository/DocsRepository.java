package com.bamboo.postService.repository;

import com.bamboo.postService.dto.blog.BlogTagView;
import com.bamboo.postService.dto.doc.DocHomeDto;
import com.bamboo.postService.entity.Docs;

import feign.Param;

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
                     d.createdAt as createdAt
                    from Docs d
                    order by d.createdAt desc
            """)
    List<DocHomeDto> findAllCoverDocs(Pageable pageable);
}
