package com.cotato.nextstation.domain.place.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 *  장소 리뷰 하나당 이미지가 여러 장 붙을 수 있어 별도 테이블로 관리 (1:N).
 *
 * 리뷰 상세/목록 조회 시 항상 리뷰와 함께 조회되므로 PlaceReview를 연관관계로 매핑.
 *  리뷰 삭제 여부와 무관하게 장소 사진 데이터는 보존.
 */
@Entity
@Table(name = "place_review_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceReviewImage extends BaseEntity {

    // 리뷰가 soft delete 되어도 사진은 남아야 하므로 nullable 허용 없이 그대로 유지하되,
    // 화면 노출 판단 시 이 필드가 아닌 place 기준으로 조회할 것
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_review_id", nullable = false)
    private PlaceReview placeReview;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public PlaceReviewImage(Place place, PlaceReview placeReview, String imageUrl) {
        this.placeReview = placeReview;
        this.imageUrl = imageUrl;
        this.createdAt = LocalDateTime.now();
    }
}