package com.cotato.nextstation.domain.station.repository;

import com.cotato.nextstation.domain.station.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {

    Optional<Station> findByStationName(String stationName);

    // 랜덤뽑기 대상(뽑기 역 50개) 조회
    List<Station> findByIsDrawableTrue();
}
