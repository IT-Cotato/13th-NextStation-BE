package com.cotato.nextstation.domain.member.entity;

import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "member_social_account",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_social_account_provider_provider_user_id",
                columnNames = {"provider", "provider_user_id"}
        ),
        indexes = @Index(name = "idx_member_social_account_member_id", columnList = "member_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSocialAccount extends BaseTimeEntity {

    // 연관관계 매핑 대신 FK 식별자(Long)만 보관
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column
    private String email;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    // TODO: 로그인마다 갱신 필요. 현재 미구현 - 갱신하려면 KakaoLoginQueryService(조회 전용, readOnly)가 쓰기를 해야 해서
    //  기존 Query/Command 서비스 분리 컨벤션과 충돌한다. 당장은 connectedAt(최초 연동 시각)만 기록한다.
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder
    private MemberSocialAccount(Long memberId, AuthProvider provider, String providerUserId, String email) {
        this.memberId = memberId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.connectedAt = LocalDateTime.now();
    }
}
