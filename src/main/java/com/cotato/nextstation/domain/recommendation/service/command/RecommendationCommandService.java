package com.cotato.nextstation.domain.recommendation.service.command;

import com.cotato.nextstation.domain.course.repository.CourseRepository;
import com.cotato.nextstation.domain.recommendation.converter.RecommendationConverter;
import com.cotato.nextstation.domain.recommendation.dto.request.CustomRecommendationRequest;
import com.cotato.nextstation.domain.recommendation.dto.response.CoursePreviewResponse;
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
import com.cotato.nextstation.domain.station.exception.StationErrorCode;
import com.cotato.nextstation.domain.station.repository.StationLineRepository;
import com.cotato.nextstation.domain.station.repository.StationLineRepository.StationLineView;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationCommandService {

    private static final String COURSE_NAME_SUFFIX = " 환승여행 코스";
    // 코스 미리보기 카테고리 노출 순서(문화공간 → 식당 → 카페 → 산책)
    private static final List<String> CATEGORY_DISPLAY_ORDER = List.of("CULTURE", "FOOD", "CAFE", "WALK");

    // 맞춤추천 점수 = 선택 태그별 장소 수 합 + (충족 태그 수 × TAG_MATCH_WEIGHT)
    // 2026-08-03 실데이터(place_0730) 시뮬레이션 결과 10 이상이면 순위가 포화되어(더 올려도 결과 동일) 그대로 둔다.
    private static final int TAG_MATCH_WEIGHT = 10;
    // 점수 상위 이 개수만 후보군으로 남긴다. 동점은 무작위로 섞는다.
    // 기존 퍼센트 컷(최고점의 90%)은 가중치를 올리면 컷 폭도 같이 늘어나 후보군 크기가 가중치에 종속됐다.
    // 개수 고정으로 바꿔 가중치·후보군 크기를 독립적으로 튜닝할 수 있게 했다(2026-08-03).
    private static final int CANDIDATE_POOL_SIZE = 5;
    // 가본 역 감점 점수. 안 가본 역을 하드 필터링하지 않고 점수만 깎아 후보군 진입 여부에만 영향을 준다.
    // 실데이터 시뮬레이션 결과 TAG_MATCH_WEIGHT(10)에 근접한 값은 하드 제외와 다를 바 없어져(top-5 내 가본역 비율 1%대) 4로 정했다(2026-08-03).
    // 단, top-5 안에 든 이후의 최종 무작위 선택은 점수와 무관하게 균등 랜덤이라, 감점은 "후보군 진입 여부"에만 영향을 주고
    // 이미 진입한 역들 사이의 선택 확률에는 영향을 주지 않는다 — 도달 가능 역이 5개 이하면 감점이 사실상 무효화된다.
    private static final int VISITED_PENALTY = 4;

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

    // 코스만 다시 뽑기. 역은 고정하고 코스 미리보기만 완전 무작위로 다시 구성한다.
    // 새로 추천된 역이 없으므로 로그를 남기지 않고,
    // 직전 결과 제외 같은 로직도 적용하지 않는다.
    public CoursePreviewResponse redrawCourse(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new CustomException(StationErrorCode.STATION_NOT_FOUND));

        // 뽑기 대상이 아닌 역은 장소가 없어 빈 코스가 나간다. 현재 데이터도 장소가 뽑기 역에만 붙어 있지만
        // StationQueryService.getStationPlaces와 같은 이유로 그 전제에 기대지 않고 조건을 명시한다.
        List<StationPlaceView> previewPlaces = station.isDrawable()
                ? selectOnePerCategory(stationPlaceReader.getPlacesByStation(stationId))
                : List.of();
        String courseName = station.getStationName() + COURSE_NAME_SUFFIX;
        return recommendationConverter.toCoursePreview(courseName, previewPlaces);
    }

    /**
     * 맞춤추천. 다음 순서로 역을 좁혀 그중 하나를 무작위로 고른다.
     * 1. 출발역에서 이동 가능 시간 내 도달 가능한 뽑기 대상 역
     * 2. 선택한 여행 스타일 태그 점수 상위 CANDIDATE_POOL_SIZE개 역(후보군) — 가본 역은 VISITED_PENALTY만큼 감점한 뒤 순위를 매긴다
     * 3. 직전 추천 역 제외 — 제외하면 후보가 비면 이 단계는 건너뛴다
     */
    public CustomRecommendationResponse recommendCustom(Long memberId, CustomRecommendationRequest request) {
        validateDepartureStation(request.departureStationId());

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

        Set<Long> visitedStationIds = Set.copyOf(courseRepository.findVisitedStationIds(memberId));
        List<Station> scoreCandidates = selectScoreCandidates(reachableStations, request.travelStyles(), visitedStationIds);
        List<Station> finalCandidates = excludeLastRecommended(scoreCandidates, memberId);

        Station picked = pickRandom(finalCandidates);
        recordCustomLog(memberId, picked.getId(), request);
        log.info("맞춤추천 완료 - memberId: {}, 출발역: {}, 이동시간: {}, 스타일: {}, 추천역: {}",
                memberId, request.departureStationId(), request.travelTime(), request.travelStyles(), picked.getId());

        List<StationLineView> lines = stationLineRepository.findLinesByStationIdIn(List.of(picked.getId()));
        return recommendationConverter.toCustomResponse(picked, lines, durationByStationId.get(picked.getId()));
    }

    private void validateDepartureStation(Long departureStationId) {
        if (!stationRepository.existsById(departureStationId)) {
            throw new CustomException(RecommendationErrorCode.DEPARTURE_STATION_NOT_FOUND);
        }
    }

    // 출발역에서 도달 가능한 뽑기 대상 역과 소요시간. 이동 시간 제한이 없으면 전 구간을 가져온다.
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

    // 점수 상위 CANDIDATE_POOL_SIZE개 역만 후보군으로 남긴다. 가본 역은 감점 후 순위를 매기고, 동점 역은 무작위로 섞어 매번 다르게 채운다.
    private List<Station> selectScoreCandidates(List<Station> stations, List<String> travelStyles, Set<Long> visitedStationIds) {
        Map<Long, Map<String, Long>> countsByStationId = stationTagCountReader.getPlaceCountsByStationForTags(travelStyles);

        List<Station> shuffled = new ArrayList<>(stations);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());
        return shuffled.stream()
                .sorted(Comparator.comparingLong(
                        (Station station) -> calculateScore(station, countsByStationId, travelStyles, visitedStationIds)).reversed())
                .limit(CANDIDATE_POOL_SIZE)
                .toList();
    }

    private long calculateScore(Station station, Map<Long, Map<String, Long>> countsByStationId, List<String> travelStyles,
                                 Set<Long> visitedStationIds) {
        long score = calculateTagScore(countsByStationId.get(station.getId()), travelStyles);
        return visitedStationIds.contains(station.getId()) ? score - VISITED_PENALTY : score;
    }

    private long calculateTagScore(Map<String, Long> countsByTag, List<String> travelStyles) {
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

    // 랜덤뽑기는 선택 조건이 없어 결과 역만 남긴다.
    private void recordLog(Long memberId, Long stationId, boolean isRandom) {
        recommendationLogRepository.save(
                RecommendationLog.builder()
                        .memberId(memberId)
                        .resultStationId(stationId)
                        .isRandom(isRandom)
                        .build()
        );
    }

    // 맞춤추천은 어떤 조건이 많이 쓰이는지 집계할 수 있도록 선택 조건까지 함께 남긴다.
    private void recordCustomLog(Long memberId, Long stationId, CustomRecommendationRequest request) {
        recommendationLogRepository.save(
                RecommendationLog.builder()
                        .memberId(memberId)
                        .resultStationId(stationId)
                        .isRandom(false)
                        .departureStationId(request.departureStationId())
                        .travelTime(request.travelTime())
                        .travelStyles(request.travelStyles())
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
