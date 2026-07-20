package com.cotato.nextstation.domain.recommendation.service.command;

import com.cotato.nextstation.domain.recommendation.converter.RecommendationConverter;
import com.cotato.nextstation.domain.recommendation.dto.response.RandomRecommendationResponse;
import com.cotato.nextstation.domain.recommendation.entity.RecommendationLog;
import com.cotato.nextstation.domain.recommendation.exception.RecommendationErrorCode;
import com.cotato.nextstation.domain.recommendation.repository.RecommendationLogRepository;
import com.cotato.nextstation.domain.recommendation.service.port.StationPlaceReader;
import com.cotato.nextstation.domain.recommendation.service.port.StationPlaceView;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationCommandService {

    private static final String COURSE_NAME_SUFFIX = " 환승여행 코스";
    // 코스 미리보기 카테고리 노출 순서(문화공간 → 식당 → 카페 → 산책)
    private static final List<String> CATEGORY_DISPLAY_ORDER = List.of("CULTURE", "FOOD", "CAFE", "WALK");

    private final StationRepository stationRepository;
    private final RecommendationLogRepository recommendationLogRepository;
    private final StationPlaceReader stationPlaceReader;
    private final RecommendationConverter recommendationConverter;

    /**
     * 랜덤뽑기(RANDOM-01). memberId가 있으면(로그인) 직전 추천 1건을 제외한다.
     */
    public RandomRecommendationResponse drawRandom(Long memberId) {
        Station picked = pickDrawableStation(memberId);
        recommendationLogRepository.save(
                RecommendationLog.builder()
                        .memberId(memberId)
                        .resultStationId(picked.getId())
                        .isRandom(true)
                        .build()
        );

        List<StationPlaceView> previewPlaces = selectOnePerCategory(stationPlaceReader.getPlacesByStation(picked.getId()));
        String courseName = picked.getStationName() + COURSE_NAME_SUFFIX;
        return recommendationConverter.toRandomResponse(picked, courseName, previewPlaces);
    }

    private Station pickDrawableStation(Long memberId) {
        List<Station> drawables = stationRepository.findByIsDrawableTrue();
        if (drawables.isEmpty()) {
            throw new CustomException(RecommendationErrorCode.NO_DRAWABLE_STATION);
        }

        List<Station> candidates = excludeLastRecommended(drawables, memberId);
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    // 로그인 사용자의 직전 추천 1건을 후보에서 제외한다. 제외 후 비면(뽑기 역이 1개뿐) 전체에서 다시 뽑는다.
    private List<Station> excludeLastRecommended(List<Station> drawables, Long memberId) {
        if (memberId == null) {
            return drawables;
        }

        Long lastStationId = recommendationLogRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .map(RecommendationLog::getResultStationId)
                .orElse(null);
        if (lastStationId == null) {
            return drawables;
        }

        List<Station> filtered = drawables.stream()
                .filter(station -> !station.getId().equals(lastStationId))
                .toList();
        return filtered.isEmpty() ? drawables : filtered;
    }

    // 카테고리 노출 순서대로 카테고리당 첫 장소 1개씩 선택한다. 장소가 없는 카테고리는 건너뛴다(COURSE-01).
    private List<StationPlaceView> selectOnePerCategory(List<StationPlaceView> places) {
        List<StationPlaceView> selected = new ArrayList<>();
        for (String categoryCode : CATEGORY_DISPLAY_ORDER) {
            places.stream()
                    .filter(place -> categoryCode.equals(place.categoryCode()))
                    .findFirst()
                    .ifPresent(selected::add);
        }
        return selected;
    }
}
