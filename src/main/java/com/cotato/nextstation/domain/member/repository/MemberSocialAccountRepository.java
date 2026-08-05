package com.cotato.nextstation.domain.member.repository;

import com.cotato.nextstation.domain.member.entity.AuthProvider;
import com.cotato.nextstation.domain.member.entity.MemberSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccount, Long> {

    Optional<MemberSocialAccount> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    // 한 회원이 여러 소셜을 연동하는 기능은 아직 없음 -> 생겨도 예외 대신 첫 건을 쓰도록 First로 조회한다.
    Optional<MemberSocialAccount> findFirstByMemberId(Long memberId);
}

