package com.cotato.nextstation.domain.station.repository;

import com.cotato.nextstation.domain.station.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {

    Optional<Station> findByStationName(String stationName);

    boolean existsByIsDrawableTrue();
}
