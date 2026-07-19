package com.cotato.nextstation.domain.auth.entity;

import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "member_terms_agreement",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_terms_agreement_member_terms",
                columnNames = {"member_id", "terms_consents_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTermsAgreement extends BaseTimeEntity {

    // 연관관계 매핑 대신 FK 식별자(Long)만 보관한다.
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "terms_consents_id", nullable = false)
    private Long termsConsentsId;

    @Column(name = "is_agreed", nullable = false)
    private boolean agreed;

    @Column(name = "ip_address")
    private String ipAddress;

    @Builder
    private MemberTermsAgreement(Long memberId, Long termsConsentsId, boolean agreed, String ipAddress) {
        this.memberId = memberId;
        this.termsConsentsId = termsConsentsId;
        this.agreed = agreed;
        this.ipAddress = ipAddress;
    }
}