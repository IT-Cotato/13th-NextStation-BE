package com.cotato.nextstation.domain.member.repository;

import com.cotato.nextstation.domain.member.entity.AuthProvider;
import com.cotato.nextstation.domain.member.entity.MemberSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccount, Long> {

    Optional<MemberSocialAccount> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    // 유예 만료 시 소셜 연결을 끊어 같은 소셜 계정으로 재가입할 수 있게 한다.
    // provider/provider_user_id가 nullable = false라 컬럼을 비우는 방식은 쓸 수 없다.
    int deleteByMemberId(Long memberId);
}

