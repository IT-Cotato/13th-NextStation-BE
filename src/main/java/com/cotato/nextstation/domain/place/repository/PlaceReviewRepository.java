package com.cotato.nextstation.domain.place.repository;

import com.cotato.nextstation.domain.place.entity.PlaceReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlaceReviewRepository extends JpaRepository<PlaceReview, Long> {

    // 장소 상세 조회 - 공개(삭제되지 않은 journal) 기준 리뷰 목록 조회
    // 리뷰 내용은 여행일지 작성 시 선택 입력이라 비어 있을 수 있으므로, 내용 없는 리뷰는 노출하지 않는다
    @Query("SELECT pr FROM PlaceReview pr " +
            "JOIN FETCH pr.journal j " +
            "JOIN FETCH j.member m " +
            "WHERE pr.place.id = :placeId " +
            "AND pr.review IS NOT NULL AND TRIM(pr.review) <> '' " +
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


    // 장소 리뷰 목록 - 최신순, 최초 페이지
    // 리뷰 내용은 여행일지 작성 시 선택 입력이라 비어 있을 수 있으므로, 내용 없는 리뷰는 노출하지 않는다
    @Query("SELECT pr FROM PlaceReview pr " +
            "JOIN FETCH pr.journal j " +
            "JOIN FETCH j.member m " +
            "WHERE pr.place.id = :placeId " +
            "AND pr.review IS NOT NULL AND TRIM(pr.review) <> '' " +
            "ORDER BY pr.createdAt DESC, pr.id DESC")
    List<PlaceReview> findByPlaceIdOrderByLatest(@Param("placeId") Long placeId, Pageable pageable);

    // 장소 리뷰 목록 - 최신순, 커서 이후 페이지
    @Query("SELECT pr FROM PlaceReview pr " +
            "JOIN FETCH pr.journal j " +
            "JOIN FETCH j.member m " +
            "WHERE pr.place.id = :placeId " +
            "AND pr.review IS NOT NULL AND TRIM(pr.review) <> '' " +
            "AND (pr.createdAt < :createdAt OR (pr.createdAt = :createdAt AND pr.id < :reviewId)) " +
            "ORDER BY pr.createdAt DESC, pr.id DESC")
    List<PlaceReview> findByPlaceIdOrderByLatestAfterCursor(
            @Param("placeId") Long placeId,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("reviewId") Long reviewId,
            Pageable pageable);

    // 장소 리뷰 목록 - 추천순(likeCount 캐시 컬럼 기준), 최초 페이지
    @Query("SELECT pr FROM PlaceReview pr " +
            "JOIN FETCH pr.journal j " +
            "JOIN FETCH j.member m " +
            "WHERE pr.place.id = :placeId " +
            "AND pr.review IS NOT NULL AND TRIM(pr.review) <> '' " +
            "ORDER BY pr.likeCount DESC, pr.id DESC")
    List<PlaceReview> findByPlaceIdOrderByRecommend(@Param("placeId") Long placeId, Pageable pageable);

    // 장소 리뷰 목록 - 추천순, 커서 이후 페이지
    @Query("SELECT pr FROM PlaceReview pr " +
            "JOIN FETCH pr.journal j " +
            "JOIN FETCH j.member m " +
            "WHERE pr.place.id = :placeId " +
            "AND pr.review IS NOT NULL AND TRIM(pr.review) <> '' " +
            "AND (pr.likeCount < :likeCount OR (pr.likeCount = :likeCount AND pr.id < :reviewId)) " +
            "ORDER BY pr.likeCount DESC, pr.id DESC")
    List<PlaceReview> findByPlaceIdOrderByRecommendAfterCursor(
            @Param("placeId") Long placeId,
            @Param("likeCount") long likeCount,
            @Param("reviewId") Long reviewId,
            Pageable pageable);

    // 장소 리뷰 총 개수 (삭제된 journal의 리뷰도 카운트에 포함되지 않도록 journal 조인 추가,
    // 내용 없는 리뷰는 노출되지 않으므로 카운트에서도 제외)
    @Query("SELECT COUNT(pr) FROM PlaceReview pr " +
            "JOIN pr.journal j " +
            "WHERE pr.place.id = :placeId " +
            "AND pr.review IS NOT NULL AND TRIM(pr.review) <> ''")
    long countByPlaceId(@Param("placeId") Long placeId);

    // 여행일지에 연결된 장소 리뷰 리스트
    List<PlaceReview> findByJournalId(Long journalId);


    Optional<PlaceReview> findByJournalIdAndPlaceId(Long journalId, Long placeId);

}