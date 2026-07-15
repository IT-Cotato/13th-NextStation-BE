package com.cotato.nextstation.domain.departure.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "member_departure_station")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MemberDepartureStation extends BaseEntity {

    // 다른 파트 소유 엔티티(member/station)는 A안에 따라 Long id 컬럼으로만 매핑한다.
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(length = 30)
    private String label;

    @Column(name = "order_num", nullable = false)
    private int orderNum;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private MemberDepartureStation(Long memberId, Long stationId, String label, int orderNum) {
        this.memberId = memberId;
        this.stationId = stationId;
        this.label = label;
        this.orderNum = orderNum;
    }
}
