package com.cotato.nextstation.domain.journal.repository;

import com.cotato.nextstation.domain.journal.entity.JournalImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JournalImageRepository extends JpaRepository<JournalImage, Long> {

    List<JournalImage> findByJournalIdOrderByIdAsc(Long journalId);


    List<JournalImage> findByJournalId(Long journal);

    Optional<JournalImage> findByIdAndJournalId(Long id, Long journalId);

    Optional<JournalImage> findFirstByJournalIdOrderByCreatedAtAsc(Long journalId);

    // 여러 일지의 사진을 한 번에 가져온다. 일지마다 첫 사진을 고르는 건 호출부가 한다.
    // 목록 화면에서 일지마다 조회하면 카드 수만큼 쿼리가 나간다.
    // 엔티티를 그대로 받으면 journal이 지연 로딩이라 일지 수만큼 추가 조회가 나가므로 id만 뽑는다.
    @Query("SELECT ji.journal.id AS journalId, ji.imageUrl AS imageUrl " +
            "FROM JournalImage ji " +
            "WHERE ji.journal.id IN :journalIds " +
            "ORDER BY ji.journal.id ASC, ji.createdAt ASC")
    List<JournalImageView> findImagesByJournalIds(@Param("journalIds") Collection<Long> journalIds);

    interface JournalImageView {
        Long getJournalId();
        String getImageUrl();
    }
}