package com.cotato.nextstation.domain.station.repository;

import com.cotato.nextstation.domain.station.entity.StationLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationLineRepository extends JpaRepository<StationLine, Long> {
}
