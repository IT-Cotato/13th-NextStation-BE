package com.cotato.nextstation.domain.journal.repository;

import com.cotato.nextstation.domain.journal.entity.JournalImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JournalImageRepository extends JpaRepository<JournalImage, Long> {

    List<JournalImage> findByJournalIdOrderByIdAsc(Long journalId);


    List<JournalImage> findByJournalId(Long journal);

    Optional<JournalImage> findByIdAndJournalId(Long id, Long journalId);

    Optional<JournalImage> findFirstByJournalIdOrderByCreatedAtAsc(Long journalId);
}