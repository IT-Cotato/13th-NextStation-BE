package com.cotato.nextstation.domain.place.repository;

import com.cotato.nextstation.domain.place.entity.PlaceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PlaceReviewRepository extends JpaRepository<PlaceReview, Long> {

    // 장소 상세 조회 - 공개(삭제되지 않은 journal) 기준 리뷰 목록 조회
    @Query("SELECT pr FROM PlaceReview pr " +
            "JOIN FETCH pr.journal j " +
            "WHERE pr.place.id = :placeId " +
            "ORDER BY pr.createdAt DESC")
    List<PlaceReview> findVisibleReviewsByPlaceId(@Param("placeId") Long placeId);
}