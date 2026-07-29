package com.cotato.nextstation.domain.journal.repository;

import com.cotato.nextstation.domain.journal.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface JournalRepository extends JpaRepository<Journal, Long> {

    boolean existsByIdAndMember_Id(Long journalId, Long memberId);

    // 이미 여행일지가 작성된 memberStampId 목록 조회
    @Query("SELECT j.memberStampId FROM Journal j WHERE j.member.id = :memberId")
    Set<Long> findCompletedMemberStampIdsByMemberId(@Param("memberId") Long memberId);

}