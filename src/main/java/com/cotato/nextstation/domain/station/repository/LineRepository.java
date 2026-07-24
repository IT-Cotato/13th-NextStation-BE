package com.cotato.nextstation.domain.station.repository;

import com.cotato.nextstation.domain.station.entity.Line;
import com.cotato.nextstation.domain.station.entity.LineCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LineRepository extends JpaRepository<Line, Long> {

    Optional<Line> findByName(String name);

    Optional<Line> findByCode(LineCode code);
}
