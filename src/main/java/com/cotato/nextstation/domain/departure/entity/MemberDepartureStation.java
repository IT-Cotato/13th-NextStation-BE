package com.cotato.nextstation.domain.departure.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "member_departure_station",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_departure_station_member_order",
                columnNames = {"member_id", "order_num"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MemberDepartureStation extends BaseEntity {

    // 연관관계 매핑 대신 FK 식별자(Long)만 보관한다.
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "order_num", nullable = false)
    private int orderNum;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private MemberDepartureStation(Long memberId, Long stationId, int orderNum) {
        this.memberId = memberId;
        this.stationId = stationId;
        this.orderNum = orderNum;
    }
}
