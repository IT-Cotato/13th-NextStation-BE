package com.cotato.nextstation.domain.route.repository;

import com.cotato.nextstation.domain.route.entity.StationRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StationRouteRepository extends JpaRepository<StationRoute, Long> {

    // 출발역에서 제한 시간 내에 도달할 수 있는 역과 그 소요시간
    @Query("SELECT r.arrivalStationId AS stationId, r.durationMinutes AS durationMinutes " +
            "FROM StationRoute r " +
            "WHERE r.departureStationId = :departureStationId " +
            "AND r.durationMinutes <= :maxDurationMinutes")
    List<ReachableStationView> findReachable(@Param("departureStationId") Long departureStationId,
                                             @Param("maxDurationMinutes") int maxDurationMinutes);

    // 이동 시간 제한이 없을 때(상관없음) 소요시간 표시용으로 전 구간을 가져온다.
    @Query("SELECT r.arrivalStationId AS stationId, r.durationMinutes AS durationMinutes " +
            "FROM StationRoute r " +
            "WHERE r.departureStationId = :departureStationId")
    List<ReachableStationView> findAllFromDeparture(@Param("departureStationId") Long departureStationId);

    interface ReachableStationView {
        Long getStationId();
        int getDurationMinutes();
    }
}
