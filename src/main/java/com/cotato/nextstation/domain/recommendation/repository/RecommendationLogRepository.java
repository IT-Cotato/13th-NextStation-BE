package com.cotato.nextstation.domain.recommendation.repository;

import com.cotato.nextstation.domain.recommendation.entity.RecommendationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendationLogRepository extends JpaRepository<RecommendationLog, Long> {

    // 직전 추천 1건(랜덤/맞춤 구분 없이 통합). 로그인 사용자에게만 조회한다.
    Optional<RecommendationLog> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);
}
