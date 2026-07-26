package com.cotato.nextstation.domain.station.repository;

import com.cotato.nextstation.domain.station.entity.LineCode;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.entity.StationLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StationLineRepository extends JpaRepository<StationLine, Long> {

    Optional<StationLine> findFirstByStation(Station station);

    // 여러 역의 소속 노선을 한 번에 조회 (N+1 방지)
    // line.id 순으로 정렬해 노선 표시 순서를 일정하게 유지한다.
    @Query("SELECT sl.station.id AS stationId, " +
            "sl.line.id AS lineId, sl.line.name AS lineName, sl.line.code AS lineCode " +
            "FROM StationLine sl " +
            "WHERE sl.station.id IN :stationIds " +
            "ORDER BY sl.station.id, sl.line.id")
    List<StationLineView> findLinesByStationIdIn(@Param("stationIds") Collection<Long> stationIds);

    interface StationLineView {
        Long getStationId();
        Long getLineId();
        String getLineName();
        LineCode getLineCode();
    }
}
