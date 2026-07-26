package com.cotato.nextstation.domain.station.service.query;

import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.service.query.PlaceInfoQueryService;
import com.cotato.nextstation.domain.place.service.query.PlaceQueryService;
import com.cotato.nextstation.domain.station.converter.StationConverter;
import com.cotato.nextstation.domain.station.dto.response.StationPlaceCategoryResponse;
import com.cotato.nextstation.domain.station.dto.response.StationPlacesResponse;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.exception.StationErrorCode;
import com.cotato.nextstation.domain.station.repository.StationLineRepository;
import com.cotato.nextstation.domain.station.repository.StationLineRepository.StationLineView;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 역 조회 전용 서비스. 다른 도메인이 역 정보가 필요할 때 이 서비스를 호출한다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StationQueryService {

    // 코스 만들기 화면의 카테고리 노출 순서(문화공간 → 식당 → 카페 → 산책포인트).
    // 랜덤뽑기 코스 미리보기와 같은 순서를 쓴다.
    private static final List<String> CATEGORY_DISPLAY_ORDER = List.of("CULTURE", "FOOD", "CAFE", "WALK");
    private static final int PLACES_PER_CATEGORY = 3;
    private static final String COURSE_NAME_SUFFIX = " 환승여행 코스";

    private final StationRepository stationRepository;
    private final StationLineRepository stationLineRepository;
    private final PlaceQueryService placeQueryService;
    private final PlaceInfoQueryService placeInfoQueryService;
    private final StationConverter stationConverter;

    /**
     * 코스 만들기 후보 장소를 카테고리별로 묶어 반환한다.
     * 카테고리당 최대 {@value #PLACES_PER_CATEGORY}개이며, 후보가 더 많으면 잘라내고 적으면 있는 만큼 내린다.
     * 랜덤뽑기와 달리 호출할 때마다 결과가 바뀌면 선택 중인 사용자가 혼란스러우므로 순서를 고정한다
     * (현재 선정 기준은 id 순).
     * 뽑기 대상이 아닌 역은 빈 목록이 나가며, 역 자체가 없으면 404다.
     */
    public StationPlacesResponse getStationPlaces(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new CustomException(StationErrorCode.STATION_NOT_FOUND));

        // 코스 만들기는 뽑기 결과 역에서만 진입하므로 뽑기 대상이 아닌 역은 후보를 내리지 않는다.
        // 현재 데이터도 장소가 뽑기 역에만 붙어 있지만, 그 전제에 기대지 않고 조건을 명시한다.
        List<PlaceInfoResponse> allPlaces = station.isDrawable()
                ? placeQueryService.getPlacesByStation(stationId)
                : List.of();
        Map<String, List<PlaceInfoResponse>> placesByCategory = allPlaces.stream()
                .collect(Collectors.groupingBy(PlaceInfoResponse::categoryCode));

        List<StationPlaceCategoryResponse> categories = new ArrayList<>();
        for (String categoryCode : CATEGORY_DISPLAY_ORDER) {
            List<PlaceInfoResponse> places = placesByCategory.getOrDefault(categoryCode, List.of());
            if (places.isEmpty()) {
                continue;
            }
            // 후보가 최대 개수보다 많으면 잘라낸다. 순서만 고정되면 되므로 현재는 id 순으로 정렬한다.
            List<PlaceInfoResponse> selected = places.stream()
                    .sorted(Comparator.comparing(PlaceInfoResponse::placeId))
                    .limit(PLACES_PER_CATEGORY)
                    .toList();
            // 카테고리 표시명은 카테고리의 속성이지만 별도 조회 창구가 없어 같은 그룹의 장소에서 가져온다.
            // 한 그룹의 장소는 모두 같은 카테고리이므로 어느 것을 써도 값은 같다.
            categories.add(stationConverter.toPlaceCategoryResponse(
                    categoryCode, selected.get(0).categoryName(), selected));
        }

        List<LineSummaryResponse> lines = groupLines(List.of(stationId)).getOrDefault(stationId, List.of());
        List<String> tags = resolveTopTags(allPlaces);
        String defaultCourseName = station.getStationName() + COURSE_NAME_SUFFIX;
        return stationConverter.toPlacesResponse(station, lines, tags, defaultCourseName, categories);
    }

    // 역 대표 태그 = 그 역 장소들의 태그를 집계한 상위 3개
    private List<String> resolveTopTags(List<PlaceInfoResponse> places) {
        if (places.isEmpty()) {
            return List.of();
        }
        List<Long> placeIds = places.stream().map(PlaceInfoResponse::placeId).toList();
        return placeInfoQueryService.getTopTagNames(placeIds);
    }

    // 역 이름 검색 (현재 전체일치). 못 찾으면 빈 목록
    public List<StationSummaryResponse> searchByName(String keyword) {
        return stationRepository.findByStationName(keyword)
                .map(station -> {
                    Map<Long, List<LineSummaryResponse>> lines = groupLines(List.of(station.getId()));
                    return List.of(stationConverter.toSummaryResponse(station, lines.getOrDefault(station.getId(), List.of())));
                })
                .orElseGet(List::of);
    }

    // 여러 역의 소속 노선을 stationId 기준으로 묶는다.
    private Map<Long, List<LineSummaryResponse>> groupLines(Collection<Long> stationIds) {
        return stationLineRepository.findLinesByStationIdIn(stationIds).stream()
                .collect(Collectors.groupingBy(
                        StationLineView::getStationId,
                        Collectors.mapping(
                                line -> new LineSummaryResponse(
                                        line.getLineId(), line.getLineName(), line.getLineCode()),
                                Collectors.toList())
                ));
    }
}
