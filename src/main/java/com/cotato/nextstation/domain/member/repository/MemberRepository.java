package com.cotato.nextstation.domain.member.repository;

import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 프로필 설정 완료 상태 전환을 선점한다. 갱신된 행 수를 반환하며, 0이면 이미 다른 요청이 전환을 끝낸 것이다.
     * 조회 시점의 status 검사만으로는 같은 signupToken으로 동시에 들어온 두 요청이 모두 통과해
     * 프로필이 덮어써지고 로그인 세션도 중복 발급되므로, 전환 자체를 조건부 갱신으로 원자화한다.
     */
    @Modifying
    @Query("UPDATE Member m SET m.status = com.cotato.nextstation.domain.member.entity.MemberStatus.ACTIVE "
            + "WHERE m.id = :memberId AND m.status = com.cotato.nextstation.domain.member.entity.MemberStatus.PENDING")
    int activateIfPending(@Param("memberId") Long memberId);
}
