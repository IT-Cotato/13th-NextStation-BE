package com.cotato.nextstation.domain.place.repository;

import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.place.entity.PlaceReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaceReviewImageRepository extends JpaRepository<PlaceReviewImage, Long> {

    List<PlaceReviewImage> findByPlaceReview(PlaceReview placeReview);

}