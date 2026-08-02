package com.cotato.nextstation.domain.route.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 출발역(서울 전역) → 도착역(뽑기 대상 역)까지의 지하철 이동 정보
// 맞춤추천에서 "이동 가능 시간 내 도달할 수 있는 역"을 걸러내는 데 사용한다.
@Entity
@Getter
@Table(
        name = "station_route",
        indexes = @Index(
                name = "idx_station_route_departure_duration",
                columnList = "departure_station_id, duration_minutes"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StationRoute extends BaseEntity {

    // 연관관계 매핑 대신 FK 식별자만 보관한다.
    @Column(name = "departure_station_id", nullable = false)
    private Long departureStationId;

    @Column(name = "arrival_station_id", nullable = false)
    private Long arrivalStationId;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "distance_meters", nullable = false)
    private int distanceMeters;

    // 환승 정보를 산정하지 못한 구간이 있을 수 있어 nullable
    @Column(name = "transfer_count")
    private Integer transferCount;

    @Builder
    private StationRoute(Long departureStationId, Long arrivalStationId,
                         int durationMinutes, int distanceMeters, Integer transferCount) {
        this.departureStationId = departureStationId;
        this.arrivalStationId = arrivalStationId;
        this.durationMinutes = durationMinutes;
        this.distanceMeters = distanceMeters;
        this.transferCount = transferCount;
    }
}
