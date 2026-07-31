package com.cotato.nextstation.domain.journal.entity;

import com.cotato.nextstation.domain.journal.enums.TravelDuration;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.stamp.entity.MemberStamp;
import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *  탈퇴/정지된 회원(Member.status)이 작성한 리뷰/일지도 별도 필터링 없이 그대로 노출한다.
 *
 */
@Entity
@Table(name = "journal",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_journal_member_stamp",
                        columnNames = {"member_stamp_id"}
                )
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Journal extends BaseTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "member_stamp_id")
    private Long memberStampId;

    @Column(nullable = false)
    private String title;

    @Column(name = "overall_review", columnDefinition = "TEXT")
    private String overallReview;

    @Column(name = "traveled_at", nullable = false)
    private LocalDate traveledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "travel_duration", nullable = false)
    private TravelDuration travelDuration;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Journal(Member member, Long memberStampId, String title,
                   String overallReview, LocalDate traveledAt,
                   TravelDuration travelDuration, boolean isPublic) {
        this.member = member;
        this.memberStampId = memberStampId;
        this.title = title;
        this.overallReview = overallReview;
        this.traveledAt = traveledAt;
        this.travelDuration = travelDuration;
        this.isPublic = isPublic;
        this.isDeleted = false;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.memberStampId = null; // 재작성 허용을 위한 null
    }

    public void update(String title, String overallReview,
                       LocalDate traveledAt, TravelDuration travelDuration, boolean isPublic) {
        this.title = title;
        this.overallReview = overallReview;
        this.traveledAt = traveledAt;
        this.travelDuration = travelDuration;
        this.isPublic = isPublic;
    }
}