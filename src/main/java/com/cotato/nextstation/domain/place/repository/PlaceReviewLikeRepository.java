package com.cotato.nextstation.domain.place.repository;

import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.place.entity.PlaceReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PlaceReviewLikeRepository extends JpaRepository<PlaceReviewLike, Long> {

    Optional<PlaceReviewLike> findByMemberIdAndPlaceReview(Long memberId, PlaceReview placeReview);

    boolean existsByMemberIdAndPlaceReview(Long memberId, PlaceReview placeReview);

    long countByPlaceReview(PlaceReview placeReview);

    @Query("SELECT prl.placeReview.id AS reviewId, COUNT(prl.id) AS likeCount " +
            "FROM PlaceReviewLike prl " +
            "WHERE prl.placeReview.id IN :reviewIds " +
            "GROUP BY prl.placeReview.id")
    List<ReviewLikeCount> countByPlaceReviewIdIn(@Param("reviewIds") List<Long> reviewIds);

    @Query("SELECT prl.placeReview.id " +
            "FROM PlaceReviewLike prl " +
            "WHERE prl.memberId = :memberId AND prl.placeReview.id IN :reviewIds")
    List<Long> findLikedReviewIdsByMemberId(@Param("memberId") Long memberId, @Param("reviewIds") List<Long> reviewIds);

    interface ReviewLikeCount {
        Long getReviewId();
        Long getLikeCount();
    }
}