package com.cotato.nextstation.domain.place.repository;

import com.cotato.nextstation.domain.place.entity.Place;
import com.cotato.nextstation.domain.place.entity.PlaceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaceImageRepository extends JpaRepository<PlaceImage, Long> {

    // 장소 상세 조회 - 대표 이미지 노출 순서대로
    List<PlaceImage> findByPlaceOrderBySortOrderAsc(Place place);
}