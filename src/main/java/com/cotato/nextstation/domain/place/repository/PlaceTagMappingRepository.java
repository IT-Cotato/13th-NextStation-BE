package com.cotato.nextstation.domain.place.repository;

import com.cotato.nextstation.domain.place.entity.Place;
import com.cotato.nextstation.domain.place.entity.PlaceTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaceTagMappingRepository extends JpaRepository<PlaceTagMapping, Long> {

    // 장소 상세 조회 - 이 장소의 태그 전부 조회 (표시용)
    List<PlaceTagMapping> findByPlace(Place place);
}