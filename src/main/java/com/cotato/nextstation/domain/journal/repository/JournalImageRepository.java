package com.cotato.nextstation.domain.journal.repository;

import com.cotato.nextstation.domain.journal.entity.JournalImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalImageRepository extends JpaRepository<JournalImage, Long> {

    List<JournalImage> findByJournalIdOrderByIdAsc(Long journalId);

    void deleteByJournalId(Long journalId);

    void deleteByIdAndJournalId(Long id, Long journalId);
}