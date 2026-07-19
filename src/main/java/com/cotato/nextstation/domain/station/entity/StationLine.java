package com.cotato.nextstation.domain.station.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Station과 Line의 N:M 관계를 풀어주는 매핑 테이블
 * 환승역은 하나의 Station이 여러 Line과 연결된다.
 */
@Entity
@Table(
        name = "station_line",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_station_line_station_line",
                columnNames = {"station_id", "line_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StationLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id", nullable = false)
    private Line line;

    public static StationLine of(Station station, Line line) {
        return new StationLine(station, line);
    }

    private StationLine(Station station, Line line) {
        this.station = station;
        this.line = line;
    }
}
