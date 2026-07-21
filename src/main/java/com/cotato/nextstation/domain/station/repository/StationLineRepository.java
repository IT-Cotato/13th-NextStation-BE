package com.cotato.nextstation.domain.station.repository;

import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.entity.StationLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StationLineRepository extends JpaRepository<StationLine, Long> {
    Optional<StationLine> findFirstByStation(Station station);
}
