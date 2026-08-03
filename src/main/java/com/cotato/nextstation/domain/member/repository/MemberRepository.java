package com.cotato.nextstation.domain.member.repository;

import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    // 유예가 끝났고 아직 파기되지 않은 탈퇴 회원 (파기 배치 대상)
    List<Member> findAllByStatusAndDeletedAtBeforeAndPurgedAtIsNull(MemberStatus status, LocalDateTime threshold);
}
