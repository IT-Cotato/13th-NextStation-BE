package com.cotato.nextstation.domain.recommendation.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Index;
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
@Table(
        name = "recommendation_log",
        // 직전 추천 1건 조회(member_id 필터 + 최신순)를 위한 복합 인덱스
        indexes = @Index(name = "idx_recommendation_log_member_created", columnList = "member_id, created_at")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RecommendationLog extends BaseEntity {

    // 연관관계 매핑 대신 FK 식별자(Long)만 보관한다.
    // 비로그인 랜덤뽑기를 대비해 memberId는 nullable.
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "result_station_id", nullable = false)
    private Long resultStationId;

    @Column(name = "is_random", nullable = false)
    private boolean isRandom;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private RecommendationLog(Long memberId, Long resultStationId, boolean isRandom) {
        this.memberId = memberId;
        this.resultStationId = resultStationId;
        this.isRandom = isRandom;
    }
}
