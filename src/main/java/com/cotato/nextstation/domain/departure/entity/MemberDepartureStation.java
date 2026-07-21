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
        // 같은 회원이 같은 역을 중복으로 저장하지 못하도록 막는다.
        // (member_id, order_num) 유니크는 제거함: order_num은 append 표시 순서일 뿐 엄격한 유니크가 불필요하고,
        // 동시에 서로 다른 역을 추가할 때 order_num이 충돌해 "중복 역"으로 오인되는 문제를 유발했다.
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_departure_station_member_station",
                columnNames = {"member_id", "station_id"}
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
