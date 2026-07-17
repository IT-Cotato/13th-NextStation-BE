package com.cotato.nextstation.domain.place.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 장소 리뷰 좋아요. (member_id, place_review_id) 조합으로 중복 좋아요 방지.
// likeCount는 별도 캐시 컬럼 없이 이 테이블을 COUNT 집계
@Entity
@Table(
        name = "place_review_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_place_review_like_member_review",
                columnNames = {"member_id", "place_review_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceReviewLike extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_review_id", nullable = false)
    private PlaceReview placeReview;

    @Builder
    public PlaceReviewLike(Long memberId, PlaceReview placeReview) {
        this.memberId = memberId;
        this.placeReview = placeReview;
    }
}