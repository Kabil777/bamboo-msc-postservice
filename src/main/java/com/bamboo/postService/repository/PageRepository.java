package com.bamboo.postService.repository;

import com.bamboo.postService.dto.doc.DocPageAccessView;
import com.bamboo.postService.entity.Pages;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PageRepository extends JpaRepository<Pages, UUID> {

    @Query(
            """
            select
                p.docId as docId,
                p.content as content,
                d.visibility as visibility,
                d.status as status
            from Pages p
            join Docs d on d.id = p.docId
            where p.id = :pageId
              and p.docId = :docId
            """)
    Optional<DocPageAccessView> findPageAccessView(
            @Param("pageId") UUID pageId, @Param("docId") UUID docId);
}
