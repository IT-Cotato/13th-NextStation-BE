package com.cotato.nextstation.domain.place.entity;

import com.cotato.nextstation.domain.place.enums.ImageSourceType;
import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장소 상세 화면에 노출되는 대표 이미지 목록.
 *
 * sourceType으로 실제 장소 촬영본(PLACE)인지, 리뷰에서 가져온 이미지(REVIEW)인지 구분.
 *  sortOrder: 대표 이미지 노출 순서, 0이 대표(썸네일) 이미지.
 */
@Entity
@Table(name = "place_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_review_id")
    private PlaceReview placeReview;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private ImageSourceType sourceType;

    @Builder
    public PlaceImage(Place place, PlaceReview placeReview, String imageUrl,
                      int sortOrder, ImageSourceType sourceType) {
        this.place = place;
        this.placeReview = placeReview;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.sourceType = sourceType;
    }
}