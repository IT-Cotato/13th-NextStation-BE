package com.cotato.nextstation.domain.member.repository;

import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    // 유예가 끝난 탈퇴 회원 (hard delete 배치 대상). 삭제 SQL이 native라 엔티티 대신 ID만 가져온다.
    @Query("SELECT m.id FROM Member m WHERE m.status = :status AND m.deletedAt < :threshold")
    List<Long> findIdsByStatusAndDeletedAtBefore(MemberStatus status, LocalDateTime threshold);
}
