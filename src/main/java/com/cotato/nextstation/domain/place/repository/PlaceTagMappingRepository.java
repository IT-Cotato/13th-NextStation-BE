package com.cotato.nextstation.domain.place.repository;

import com.cotato.nextstation.domain.place.entity.Place;
import com.cotato.nextstation.domain.place.entity.PlaceTagMapping;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaceTagMappingRepository extends JpaRepository<PlaceTagMapping, Long> {

    // 장소 상세 조회 - 이 장소의 태그 전부 조회 (표시용)
    @EntityGraph(attributePaths = {"placeTag"})
    List<PlaceTagMapping> findByPlace(Place place);

    // Course 조회 전용 - placeIds 여러 개의 태그를 한 번에 조회
    @EntityGraph(attributePaths = {"placeTag"})
    List<PlaceTagMapping> findByPlaceIdIn(List<Long> placeIds);
}