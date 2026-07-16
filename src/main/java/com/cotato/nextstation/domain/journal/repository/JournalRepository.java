package com.cotato.nextstation.domain.journal.repository;

import com.cotato.nextstation.domain.journal.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalRepository extends JpaRepository<Journal, Long> {
}