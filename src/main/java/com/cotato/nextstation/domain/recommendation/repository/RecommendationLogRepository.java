package com.cotato.nextstation.domain.recommendation.repository;

import com.cotato.nextstation.domain.recommendation.entity.RecommendationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendationLogRepository extends JpaRepository<RecommendationLog, Long> {

    // 직전 추천 1건(랜덤/맞춤 구분 없이 통합). 로그인 사용자에게만 조회한다.
    // created_at이 같은 순간에 겹칠 때를 대비해 id로 tie-break 한다.
    Optional<RecommendationLog> findTopByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);
}
