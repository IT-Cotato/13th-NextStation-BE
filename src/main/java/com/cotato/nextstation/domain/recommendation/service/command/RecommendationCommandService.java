package com.cotato.nextstation.domain.recommendation.service.command;

import com.cotato.nextstation.domain.course.repository.CourseRepository;
import com.cotato.nextstation.domain.place.enums.PlaceTagName;
import com.cotato.nextstation.domain.recommendation.converter.RecommendationConverter;
import com.cotato.nextstation.domain.recommendation.dto.request.CustomRecommendationRequest;
import com.cotato.nextstation.domain.recommendation.dto.response.CustomRecommendationResponse;
import com.cotato.nextstation.domain.recommendation.dto.response.RandomRecommendationResponse;
import com.cotato.nextstation.domain.recommendation.entity.RecommendationLog;
import com.cotato.nextstation.domain.recommendation.enums.TravelTime;
import com.cotato.nextstation.domain.recommendation.exception.RecommendationErrorCode;
import com.cotato.nextstation.domain.recommendation.repository.RecommendationLogRepository;
import com.cotato.nextstation.domain.recommendation.service.port.StationPlaceReader;
import com.cotato.nextstation.domain.recommendation.service.port.StationPlaceView;
import com.cotato.nextstation.domain.recommendation.service.port.StationTagCountReader;
import com.cotato.nextstation.domain.route.repository.StationRouteRepository;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.repository.StationLineRepository;
import com.cotato.nextstation.domain.station.repository.StationLineRepository.StationLineView;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationCommandService {

    private static final String COURSE_NAME_SUFFIX = " 환승여행 코스";
    // 코스 미리보기 카테고리 노출 순서(문화공간 → 식당 → 카페 → 산책)
    private static final List<String> CATEGORY_DISPLAY_ORDER = List.of("CULTURE", "FOOD", "CAFE", "WALK");

    // 맞춤추천 점수 = 선택 태그별 장소 수 합 + (충족 태그 수 × TAG_MATCH_WEIGHT)
    // 실데이터에서 후보군이 지나치게 넓어지는 경향이 있어 튜닝 대상이라 상수로 분리해 둔다.
    private static final int TAG_MATCH_WEIGHT = 10;
    // 최고 점수 대비 이 비율 이상이면 후보군에 포함한다.
    private static final double CANDIDATE_SCORE_RATIO = 0.9;

    private final StationRepository stationRepository;
    private final StationLineRepository stationLineRepository;
    private final StationRouteRepository stationRouteRepository;
    private final CourseRepository courseRepository;
    private final RecommendationLogRepository recommendationLogRepository;
    private final StationPlaceReader stationPlaceReader;
    private final StationTagCountReader stationTagCountReader;
    private final RecommendationConverter recommendationConverter;

    // 랜덤뽑기. memberId가 있으면 직전 추천 1건을 제외한다.
    public RandomRecommendationResponse drawRandom(Long memberId) {
        Station picked = pickDrawableStation(memberId);
        recordLog(memberId, picked.getId(), true);

        // 환승역이면 결과 화면에 소속 노선을 모두 칩으로 노출하므로 대표 노선만이 아니라 전체를 조회한다.
        List<StationLineView> lines = stationLineRepository.findLinesByStationIdIn(List.of(picked.getId()));
        List<StationPlaceView> previewPlaces = selectOnePerCategory(stationPlaceReader.getPlacesByStation(picked.getId()));
        String courseName = picked.getStationName() + COURSE_NAME_SUFFIX;
        return recommendationConverter.toRandomResponse(picked, lines, courseName, previewPlaces);
    }

    /**
     * 맞춤추천. 다음 순서로 역을 좁혀 그중 하나를 무작위로 고른다.
     * 1. 출발역에서 이동 가능 시간 내 도달 가능한 뽑기 대상 역
     * 2. 선택한 여행 스타일 태그 점수가 최고점의 90% 이상인 역(후보군)
     * 3. (있다면) 안 가본 역만 남기기 — 전부 가본 역이면 이 단계는 건너뛴다
     * 4. (로그인 시) 직전 추천 역 제외 — 제외하면 후보가 비면 이 단계도 건너뛴다
     */
    public CustomRecommendationResponse recommendCustom(Long memberId, CustomRecommendationRequest request) {
        validateDepartureStation(request.departureStationId());
        List<String> travelStyles = validateTravelStyles(request.travelStyles());

        Map<Long, Integer> durationByStationId = findReachableDurations(request.departureStationId(), request.travelTime());
        if (durationByStationId.isEmpty()) {
            throw new CustomException(RecommendationErrorCode.NO_REACHABLE_STATION);
        }

        List<Station> reachableStations = stationRepository.findAllById(durationByStationId.keySet()).stream()
                .filter(Station::isDrawable)
                .toList();
        if (reachableStations.isEmpty()) {
            throw new CustomException(RecommendationErrorCode.NO_REACHABLE_STATION);
        }

        List<Station> scoreCandidates = selectScoreCandidates(reachableStations, travelStyles);
        List<Station> unvisited = excludeVisited(scoreCandidates, memberId);
        List<Station> finalCandidates = excludeLastRecommended(unvisited, memberId);

        Station picked = pickRandom(finalCandidates);
        recordLog(memberId, picked.getId(), false);

        List<StationLineView> lines = stationLineRepository.findLinesByStationIdIn(List.of(picked.getId()));
        return recommendationConverter.toCustomResponse(picked, lines, durationByStationId.get(picked.getId()));
    }

    private void validateDepartureStation(Long departureStationId) {
        if (!stationRepository.existsById(departureStationId)) {
            throw new CustomException(RecommendationErrorCode.DEPARTURE_STATION_NOT_FOUND);
        }
    }

    // 태그는 PlaceTagName에 존재해야 하고 중복될 수 없다.
    private List<String> validateTravelStyles(List<String> travelStyles) {
        Set<String> distinct = new LinkedHashSet<>(travelStyles);
        if (distinct.size() != travelStyles.size()) {
            throw new CustomException(RecommendationErrorCode.DUPLICATE_TRAVEL_STYLE);
        }

        for (String travelStyle : distinct) {
            try {
                PlaceTagName.valueOf(travelStyle);
            } catch (IllegalArgumentException e) {
                throw new CustomException(RecommendationErrorCode.INVALID_TRAVEL_STYLE);
            }
        }
        return List.copyOf(distinct);
    }

    // 출발역에서 도달 가능한 뽑기 대상 역과 소요시간. 이동 시간 제한이 없으면(상관없음) 전 구간을 가져온다.
    private Map<Long, Integer> findReachableDurations(Long departureStationId, TravelTime travelTime) {
        List<StationRouteRepository.ReachableStationView> routes = travelTime.hasLimit()
                ? stationRouteRepository.findReachable(departureStationId, travelTime.getMaxDurationMinutes())
                : stationRouteRepository.findAllFromDeparture(departureStationId);

        Map<Long, Integer> durationByStationId = new HashMap<>();
        for (StationRouteRepository.ReachableStationView route : routes) {
            durationByStationId.put(route.getStationId(), route.getDurationMinutes());
        }
        return durationByStationId;
    }

    // 최고 점수의 CANDIDATE_SCORE_RATIO 이상인 역만 후보군으로 남긴다.
    private List<Station> selectScoreCandidates(List<Station> stations, List<String> travelStyles) {
        Map<Long, Map<String, Long>> countsByStationId = stationTagCountReader.getPlaceCountsByStationForTags(travelStyles);

        Map<Long, Long> scoreByStationId = new HashMap<>();
        long maxScore = 0;
        for (Station station : stations) {
            long score = calculateScore(countsByStationId.get(station.getId()), travelStyles);
            scoreByStationId.put(station.getId(), score);
            maxScore = Math.max(maxScore, score);
        }

        // 어느 역도 태그에 걸리지 않으면(전부 0점) 도달 가능한 역 전체를 후보로 둔다.
        double threshold = maxScore * CANDIDATE_SCORE_RATIO;
        return stations.stream()
                .filter(station -> scoreByStationId.get(station.getId()) >= threshold)
                .toList();
    }

    private long calculateScore(Map<String, Long> countsByTag, List<String> travelStyles) {
        if (countsByTag == null) {
            return 0;
        }

        long placeCountSum = 0;
        int matchedTagCount = 0;
        for (String travelStyle : travelStyles) {
            long count = countsByTag.getOrDefault(travelStyle, 0L);
            placeCountSum += count;
            if (count > 0) {
                matchedTagCount++;
            }
        }
        return placeCountSum + (long) matchedTagCount * TAG_MATCH_WEIGHT;
    }

    // 안 가본 역(스탬프 완료 이력 없는 역)만 남긴다. 전부 가본 역이면 걸러내지 않는다.
    private List<Station> excludeVisited(List<Station> stations, Long memberId) {
        if (memberId == null) {
            return stations;
        }

        Set<Long> visitedStationIds = Set.copyOf(courseRepository.findVisitedStationIds(memberId));
        if (visitedStationIds.isEmpty()) {
            return stations;
        }

        List<Station> filtered = stations.stream()
                .filter(station -> !visitedStationIds.contains(station.getId()))
                .toList();
        return filtered.isEmpty() ? stations : filtered;
    }

    private Station pickDrawableStation(Long memberId) {
        List<Station> drawables = stationRepository.findByIsDrawableTrue();
        if (drawables.isEmpty()) {
            throw new CustomException(RecommendationErrorCode.NO_DRAWABLE_STATION);
        }
        return pickRandom(excludeLastRecommended(drawables, memberId));
    }

    // 로그인 사용자의 직전 추천 1건을 후보에서 제외한다. 제외 후 비면 전체에서 다시 뽑는다.
    private List<Station> excludeLastRecommended(List<Station> stations, Long memberId) {
        if (memberId == null) {
            return stations;
        }

        Long lastStationId = recommendationLogRepository.findTopByMemberIdOrderByCreatedAtDescIdDesc(memberId)
                .map(RecommendationLog::getResultStationId)
                .orElse(null);
        if (lastStationId == null) {
            return stations;
        }

        List<Station> filtered = stations.stream()
                .filter(station -> !station.getId().equals(lastStationId))
                .toList();
        return filtered.isEmpty() ? stations : filtered;
    }

    private Station pickRandom(List<Station> stations) {
        return stations.get(ThreadLocalRandom.current().nextInt(stations.size()));
    }

    private void recordLog(Long memberId, Long stationId, boolean isRandom) {
        recommendationLogRepository.save(
                RecommendationLog.builder()
                        .memberId(memberId)
                        .resultStationId(stationId)
                        .isRandom(isRandom)
                        .build()
        );
    }

    // 카테고리 노출 순서대로 카테고리당 1개씩 선택한다. 장소가 없는 카테고리는 건너뛴다.
    // 같은 역이 다시 뽑혀도 코스가 고정되지 않도록 카테고리 안에서는 무작위로 고른다.
    private List<StationPlaceView> selectOnePerCategory(List<StationPlaceView> places) {
        List<StationPlaceView> selected = new ArrayList<>();
        for (String categoryCode : CATEGORY_DISPLAY_ORDER) {
            List<StationPlaceView> candidates = places.stream()
                    .filter(place -> categoryCode.equals(place.categoryCode()))
                    .toList();
            if (!candidates.isEmpty()) {
                selected.add(candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())));
            }
        }
        return selected;
    }
}
