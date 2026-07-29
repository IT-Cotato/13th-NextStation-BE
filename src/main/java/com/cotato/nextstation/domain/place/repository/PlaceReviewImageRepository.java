package com.cotato.nextstation.domain.place.repository;

import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.place.entity.PlaceReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaceReviewImageRepository extends JpaRepository<PlaceReviewImage, Long> {

    List<PlaceReviewImage> findByPlaceReview(PlaceReview placeReview);

    @Query("SELECT pri FROM PlaceReviewImage pri WHERE pri.placeReview.id IN :reviewIds")
    List<PlaceReviewImage> findByPlaceReviewIdIn(@Param("reviewIds") List<Long> reviewIds);

    void deleteByPlaceReview_JournalId(Long journalId);

}