package com.cotato.nextstation.domain.station.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "station")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Station extends BaseEntity {

    @Column(name = "station_name", nullable = false, unique = true)
    private String stationName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_drawable", nullable = false)
    private boolean isDrawable;

    // 환승역이어도 뽑기 결과에는 이 노선 하나만 표시한다. 출발역 기능은 StationLine 전체를 그대로 쓴다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draw_line_id")
    private Line drawLine;

    private String todo;

    @Builder
    private Station(String stationName, String description, boolean isDrawable, String todo) {
        this.stationName = stationName;
        this.description = description;
        this.isDrawable = isDrawable;
        this.todo = todo;
    }

    public void assignAsDrawable(Line drawLine, String todo) {
        this.isDrawable = true;
        this.drawLine = drawLine;
        this.todo = todo;
    }
}
