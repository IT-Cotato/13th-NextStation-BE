package com.cotato.nextstation.domain.journal.entity;

import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 *  탈퇴/정지된 회원(Member.status)이 작성한 리뷰/일지도 별도 필터링 없이 그대로 노출한다.
 *
 */
@Entity
@Table(name = "journal")
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Journal extends BaseTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Journal(Member member, boolean isPublic) {
        this.member = member;
        this.isPublic = isPublic;
        this.isDeleted = false;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}