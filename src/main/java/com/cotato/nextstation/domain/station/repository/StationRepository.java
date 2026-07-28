package com.cotato.nextstation.domain.station.repository;

import com.cotato.nextstation.domain.station.entity.Station;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {

    Optional<Station> findByStationName(String stationName);

    // 역 검색(부분일치). "십리"로 왕십리역·답십리역·상왕십리역이 모두 걸린다.
    // "역" 같은 짧은 검색어는 결과가 지나치게 많아 Pageable로 상한을 둔다.
    List<Station> findByStationNameContainingOrderByStationNameAsc(String keyword, Pageable pageable);

    // 랜덤뽑기 대상(뽑기 역 50개) 조회
    List<Station> findByIsDrawableTrue();
}
