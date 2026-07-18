package com.cotato.nextstation.domain.place.entity;

import com.cotato.nextstation.global.entity.BaseTimeEntity;
import com.cotato.nextstation.domain.journal.entity.Journal;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "place_review",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_place_review_journal_place",
                columnNames = {"journal_id", "place_id"}
        )
)
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceReview extends BaseTimeEntity {

    /**
     * 장소 상세 조회, 리뷰 목록 조회 시 place 정보를 함께 조인해서 가져와야 하는 경우가 많아
     *  연관관계로 매핑
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    /**
     * journal.isDeleted 여부로 리뷰 노출을 판단 (공개여부는 journal.isPublic과 무관, 삭제여부만 확인)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private Journal journal;

    @Column
    private String review;

    @Column(name = "like_count", nullable = false)
    private long likeCount = 0;


    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public PlaceReview(Place place, Journal journal, String review) {
        this.place = place;
        this.journal = journal;
        this.review = review;
        this.isDeleted = false;
        this.likeCount = 0;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

}