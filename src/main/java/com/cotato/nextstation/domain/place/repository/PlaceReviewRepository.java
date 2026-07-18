package com.cotato.nextstation.domain.place.repository;

import com.cotato.nextstation.domain.place.entity.PlaceReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PlaceReviewRepository extends JpaRepository<PlaceReview, Long> {

    // 장소 상세 조회 - 공개(삭제되지 않은 journal) 기준 리뷰 목록 조회
    @Query("SELECT pr FROM PlaceReview pr " +
            "JOIN FETCH pr.journal j " +
            "JOIN FETCH j.member m " +
            "WHERE pr.place.id = :placeId " +
            "ORDER BY pr.createdAt DESC")
    List<PlaceReview> findVisibleReviewsByPlaceId(@Param("placeId") Long placeId, Pageable pageable);

    // 좋아요 추가 시 원자적 증가 (동시성 안전)
    @Modifying
    @Query("UPDATE PlaceReview pr SET pr.likeCount = pr.likeCount + 1 WHERE pr.id = :reviewId")
    void incrementLikeCount(@Param("reviewId") Long reviewId);

    // 좋아요 취소 시 원자적 감소 (0 미만으로 안 내려가도록 방어)
    @Modifying
    @Query("UPDATE PlaceReview pr SET pr.likeCount = CASE WHEN pr.likeCount > 0 THEN pr.likeCount - 1 ELSE 0 END WHERE pr.id = :reviewId")
    void decrementLikeCount(@Param("reviewId") Long reviewId);

}