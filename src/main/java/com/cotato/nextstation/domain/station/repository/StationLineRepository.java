package com.cotato.nextstation.domain.station.repository;

import com.cotato.nextstation.domain.station.entity.StationLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface StationLineRepository extends JpaRepository<StationLine, Long> {

    // 여러 역의 소속 노선명을 한 번에 조회 (역 검색 결과·출발역 목록의 lines[] 구성용, N+1 방지)
    @Query("SELECT sl.station.id AS stationId, sl.line.name AS lineName " +
            "FROM StationLine sl " +
            "WHERE sl.station.id IN :stationIds")
    List<StationLineNameView> findLineNamesByStationIdIn(@Param("stationIds") Collection<Long> stationIds);

    interface StationLineNameView {
        Long getStationId();
        String getLineName();
    }
}
