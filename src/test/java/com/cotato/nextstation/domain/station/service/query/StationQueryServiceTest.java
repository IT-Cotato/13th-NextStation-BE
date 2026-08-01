package com.cotato.nextstation.domain.station.service.query;

import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.service.query.PlaceInfoQueryService;
import com.cotato.nextstation.domain.place.service.query.PlaceQueryService;
import com.cotato.nextstation.domain.station.converter.LineConverter;
import com.cotato.nextstation.domain.station.converter.StationConverter;
import com.cotato.nextstation.domain.station.dto.response.StationPlaceCategoryResponse;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.LineCode;
import com.cotato.nextstation.domain.station.repository.StationLineRepository.StationLineView;
import com.cotato.nextstation.domain.station.repository.StationLineRepository;
import com.cotato.nextstation.domain.station.exception.StationErrorCode;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StationQueryServiceTest {

    @InjectMocks
    private StationQueryService stationQueryService;

    @Mock
    private StationRepository stationRepository;
    @Mock
    private StationLineRepository stationLineRepository;
    @Mock
    private PlaceQueryService placeQueryService;
    @Mock
    private PlaceInfoQueryService placeInfoQueryService;
    @Mock
    private StationConverter stationConverter;
    // 노선 요약 변환은 단순 매핑이라 실제 구현을 그대로 쓴다
    @Spy
    private LineConverter lineConverter = new LineConverter();

    private StationLineView lineView(Long stationId, LineCode lineCode) {
        StationLineView view = mock(StationLineView.class);
        given(view.getStationId()).willReturn(stationId);
        given(view.getLineId()).willReturn((long) lineCode.ordinal() + 1);
        given(view.getLineName()).willReturn(lineCode.getDisplayName());
        given(view.getLineCode()).willReturn(lineCode);
        return view;
    }

    @Test
    @DisplayName("검색된 역의 소속 노선을 모두 묶어 요약을 반환한다")
    void searchByName_found() {
        Station station = mock(Station.class);
        given(station.getId()).willReturn(42L);
        StationLineView view2 = lineView(42L, LineCode.LINE_2);
        StationLineView view5 = lineView(42L, LineCode.LINE_5);
        given(stationRepository.searchByNormalizedName(eq("왕십리"), eq("왕십리"), any(Pageable.class)))
                .willReturn(List.of(station));
        given(stationLineRepository.findLinesByStationIdIn(List.of(42L)))
                .willReturn(List.of(view2, view5));

        StationSummaryResponse expected = new StationSummaryResponse(42L, "왕십리역", List.of());
        ArgumentCaptor<List<LineSummaryResponse>> linesCaptor = ArgumentCaptor.forClass(List.class);
        given(stationConverter.toSummaryResponse(eq(station), linesCaptor.capture())).willReturn(expected);

        // when
        List<StationSummaryResponse> result = stationQueryService.searchByName("왕십리역");

        // then: id·name·code가 projection→service 변환에서 모두 올바르게 매핑되는지 객체 전체로 검증한다
        assertThat(result).containsExactly(expected);
        assertThat(linesCaptor.getValue()).containsExactly(
                new LineSummaryResponse(2L, "2호선", LineCode.LINE_2),
                new LineSummaryResponse(5L, "5호선", LineCode.LINE_5));
    }

    @Test
    @DisplayName("부분일치로 검색하면 해당 글자를 포함한 역이 모두 나온다")
    void searchByName_partialMatch() {
        // given: "십리"로 검색하면 왕십리역·답십리역이 모두 걸린다
        Station wangsimni = mock(Station.class);
        Station dapsimni = mock(Station.class);
        given(wangsimni.getId()).willReturn(42L);
        given(dapsimni.getId()).willReturn(43L);
        given(stationRepository.searchByNormalizedName(eq("십리"), eq("십리"), any(Pageable.class)))
                .willReturn(List.of(dapsimni, wangsimni));
        given(stationLineRepository.findLinesByStationIdIn(List.of(43L, 42L))).willReturn(List.of());

        StationSummaryResponse dap = new StationSummaryResponse(43L, "답십리역", List.of());
        StationSummaryResponse wang = new StationSummaryResponse(42L, "왕십리역", List.of());
        given(stationConverter.toSummaryResponse(eq(dapsimni), any())).willReturn(dap);
        given(stationConverter.toSummaryResponse(eq(wangsimni), any())).willReturn(wang);

        // when
        List<StationSummaryResponse> result = stationQueryService.searchByName("십리");

        // then: 리포지토리가 돌려준 순서(역명 오름차순)를 그대로 유지한다
        assertThat(result).containsExactly(dap, wang);
    }

    @Test
    @DisplayName("검색 결과는 20개로 제한한다")
    void searchByName_limitedTo20() {
        // given
        given(stationRepository.searchByNormalizedName(eq("왕십리"), eq("왕십리"), any(Pageable.class)))
                .willReturn(List.of());

        // when
        stationQueryService.searchByName("왕십리");

        // then: 짧은 검색어에 결과가 쏟아지지 않도록 상한을 넘긴다
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(stationRepository).searchByNormalizedName(eq("왕십리"), eq("왕십리"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("검색어 앞뒤 공백은 제거하고 검색한다")
    void searchByName_trimsKeyword() {
        // given
        given(stationRepository.searchByNormalizedName(eq("왕십리"), eq("왕십리"), any(Pageable.class)))
                .willReturn(List.of());

        // when
        stationQueryService.searchByName("  왕십리  ");

        // then
        verify(stationRepository).searchByNormalizedName(eq("왕십리"), eq("왕십리"), any(Pageable.class));
    }

    @Test
    @DisplayName("역명 끝의 \"역\"은 떼고 검색하므로 \"왕십리역\"과 \"왕십리\"가 같은 검색어가 된다")
    void searchByName_stripsNameSuffix() {
        // given
        given(stationRepository.searchByNormalizedName(eq("왕십리"), eq("왕십리"), any(Pageable.class)))
                .willReturn(List.of());

        // when
        stationQueryService.searchByName("왕십리역");

        // then
        verify(stationRepository).searchByNormalizedName(eq("왕십리"), eq("왕십리"), any(Pageable.class));
    }

    @Test
    @DisplayName("이름 안쪽의 \"역\"은 남기므로 \"역삼역\"은 \"역삼\"으로 검색된다")
    void searchByName_keepsInnerSuffixCharacter() {
        // given: 꼬리의 "역"만 한 번 뗀다. 앞의 "역"까지 떼면 역삼역·역촌역을 못 찾는다
        given(stationRepository.searchByNormalizedName(eq("역삼"), eq("역삼"), any(Pageable.class)))
                .willReturn(List.of());

        // when
        stationQueryService.searchByName("역삼역");

        // then
        verify(stationRepository).searchByNormalizedName(eq("역삼"), eq("역삼"), any(Pageable.class));
    }

    @Test
    @DisplayName("\"역\" 한 글자는 떼지 않고 그대로 검색해 이름 안쪽에 \"역\"이 든 역을 찾는다")
    void searchByName_onlyNameSuffix() {
        // given: 여기서 "역"을 떼면 빈 검색어가 되어 역삼역·동대문역사문화공원역을 못 찾는다.
        // 역명은 이미 꼬리를 뗀 상태로 비교하므로 이름 안쪽에 "역"이 든 역만 걸린다
        given(stationRepository.searchByNormalizedName(eq("역"), eq("역"), any(Pageable.class)))
                .willReturn(List.of());

        // when
        stationQueryService.searchByName("역");

        // then
        verify(stationRepository).searchByNormalizedName(eq("역"), eq("역"), any(Pageable.class));
    }

    @Test
    @DisplayName("검색어의 LIKE 와일드카드는 이스케이프해서 넘긴다")
    void searchByName_escapesLikeWildcard() {
        // given: 이스케이프하지 않으면 "%" 한 글자로 전체 역이 조회된다
        given(stationRepository.searchByNormalizedName(eq("100%_"), eq("100!%!_"), any(Pageable.class)))
                .willReturn(List.of());

        // when
        stationQueryService.searchByName("100%_");

        // then: 완전일치 비교에는 원본이, LIKE에는 이스케이프된 값이 넘어간다
        verify(stationRepository).searchByNormalizedName(eq("100%_"), eq("100!%!_"), any(Pageable.class));
    }

    @Test
    @DisplayName("검색어가 비어 있으면 조회하지 않고 빈 목록을 반환한다")
    void searchByName_blankKeyword() {
        // when
        List<StationSummaryResponse> result = stationQueryService.searchByName("   ");

        // then: 전체 역을 훑는 낭비를 막는다
        assertThat(result).isEmpty();
        verify(stationRepository, never())
                .searchByNormalizedName(any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("일치하는 역이 없으면 빈 목록을 반환한다")
    void searchByName_notFound() {
        // given
        given(stationRepository.searchByNormalizedName(eq("없는곳"), eq("없는곳"), any(Pageable.class)))
                .willReturn(List.of());

        // when
        List<StationSummaryResponse> result = stationQueryService.searchByName("없는곳");

        // then
        assertThat(result).isEmpty();
        verify(stationConverter, never()).toSummaryResponse(any(), any());
    }

    // ---------- 역별 장소 목록 ----------

    private Station station(Long id, String name) {
        return station(id, name, true);
    }

    private Station station(Long id, String name, boolean isDrawable) {
        Station station = Station.builder().stationName(name).isDrawable(isDrawable).build();
        ReflectionTestUtils.setField(station, "id", id);
        return station;
    }

    private PlaceInfoResponse place(Long id, String categoryCode, String categoryName) {
        return new PlaceInfoResponse(id, "장소" + id, "설명", categoryCode, categoryName, "img", 127.0, 37.5);
    }

    @Test
    @DisplayName("장소를 카테고리 순서(문화/식당/카페/산책)대로 묶고 카테고리당 3개까지 id 순으로 고른다")
    void getStationPlaces_groupsByCategoryInOrder() {
        // given: 순서를 섞고 CULTURE는 4개(3개로 잘려야 함), WALK는 없음
        given(stationRepository.findById(6L)).willReturn(Optional.of(station(6L, "보문역")));
        given(placeQueryService.getPlacesByStation(6L)).willReturn(List.of(
                place(50L, "CAFE", "카페"),
                place(14L, "CULTURE", "문화공간"),
                place(11L, "CULTURE", "문화공간"),
                place(30L, "FOOD", "식당"),
                place(13L, "CULTURE", "문화공간"),
                place(12L, "CULTURE", "문화공간")
        ));

        // when
        stationQueryService.getStationPlaces(6L, null);

        // then: 카테고리가 정해진 순서로, 없는 WALK는 빠진 채 전달된다
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<PlaceInfoResponse>> placesCaptor = ArgumentCaptor.forClass(List.class);
        verify(stationConverter, times(3))
                .toPlaceCategoryResponse(codeCaptor.capture(), nameCaptor.capture(), placesCaptor.capture());
        assertThat(codeCaptor.getAllValues()).containsExactly("CULTURE", "FOOD", "CAFE");
        // 카테고리 표시명이 코드와 짝이 맞는지까지 확인한다 (엉뚱한 이름이 들어가면 잡히도록)
        assertThat(nameCaptor.getAllValues()).containsExactly("문화공간", "식당", "카페");

        // CULTURE는 4개 중 id 작은 3개만, id 순으로
        assertThat(placesCaptor.getAllValues().get(0))
                .extracting(PlaceInfoResponse::placeId)
                .containsExactly(11L, 12L, 13L);
    }

    @Test
    @DisplayName("기본 코스 이름을 역 이름으로 만들어 함께 내려준다")
    void getStationPlaces_defaultCourseName() {
        // given
        Station station = station(6L, "보문역");
        given(stationRepository.findById(6L)).willReturn(Optional.of(station));
        given(placeQueryService.getPlacesByStation(6L)).willReturn(List.of());

        // when
        stationQueryService.getStationPlaces(6L, null);

        // then
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(stationConverter).toPlacesResponse(eq(station), any(), any(), nameCaptor.capture(), any());
        assertThat(nameCaptor.getValue()).isEqualTo("보문역 환승여행 코스");
    }

    @Test
    @DisplayName("장소가 아직 없는 역은 빈 목록으로 응답한다")
    void getStationPlaces_noPlaces() {
        // given
        given(stationRepository.findById(300L)).willReturn(Optional.of(station(300L, "서울역")));
        given(placeQueryService.getPlacesByStation(300L)).willReturn(List.of());

        // when
        stationQueryService.getStationPlaces(300L, null);

        // then: 카테고리 변환은 한 번도 일어나지 않고, 빈 목록으로 응답이 만들어진다
        verify(stationConverter, never()).toPlaceCategoryResponse(any(), any(), any());
        ArgumentCaptor<List<StationPlaceCategoryResponse>> categoriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(stationConverter).toPlacesResponse(any(), any(), any(), any(), categoriesCaptor.capture());
        assertThat(categoriesCaptor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("소속 노선과 역 대표 태그를 함께 내려준다")
    void getStationPlaces_includesLinesAndTags() {
        // given: 코스 만들기 화면 상단에 노선(6호선·우이신설선)과 태그가 표시된다
        // lineView가 내부에서 스터빙하므로 given(...) 안에서 호출하면 중첩 스터빙이 된다. 미리 만들어 둔다.
        StationLineView line6 = lineView(6L, LineCode.LINE_6);
        StationLineView lineUi = lineView(6L, LineCode.UI_SINSEOL);
        given(stationRepository.findById(6L)).willReturn(Optional.of(station(6L, "보문역")));
        List<PlaceInfoResponse> places = List.of(place(11L, "CULTURE", "문화공간"), place(30L, "FOOD", "식당"));
        given(placeQueryService.getPlacesByStation(6L)).willReturn(places);
        given(stationLineRepository.findLinesByStationIdIn(List.of(6L)))
                .willReturn(List.of(line6, lineUi));
        given(placeInfoQueryService.getTopTagNames(List.of(11L, 30L)))
                .willReturn(List.of("LOCAL_EXPLORE", "NATURE", "BUDGET"));

        // when
        stationQueryService.getStationPlaces(6L, null);

        // then
        ArgumentCaptor<List<LineSummaryResponse>> linesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> tagsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stationConverter).toPlacesResponse(any(), linesCaptor.capture(), tagsCaptor.capture(), any(), any());
        assertThat(linesCaptor.getValue()).extracting(LineSummaryResponse::code)
                .containsExactly(LineCode.LINE_6, LineCode.UI_SINSEOL);
        assertThat(tagsCaptor.getValue()).containsExactly("LOCAL_EXPLORE", "NATURE", "BUDGET");
    }

    @Test
    @DisplayName("장소가 없으면 태그 조회를 호출하지 않고 빈 태그를 내려준다")
    void getStationPlaces_noPlacesSkipsTagLookup() {
        // given
        given(stationRepository.findById(300L)).willReturn(Optional.of(station(300L, "서울역")));
        given(placeQueryService.getPlacesByStation(300L)).willReturn(List.of());

        // when
        stationQueryService.getStationPlaces(300L, null);

        // then: 빈 id 목록으로 태그를 조회하는 낭비를 막는다
        verify(placeInfoQueryService, never()).getTopTagNames(any());
        ArgumentCaptor<List<String>> tagsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stationConverter).toPlacesResponse(any(), any(), tagsCaptor.capture(), any(), any());
        assertThat(tagsCaptor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("뽑기 대상이 아닌 역은 장소가 있어도 후보로 내보내지 않는다")
    void getStationPlaces_notDrawable() {
        // given: 출발역 전용 역에 장소 데이터가 붙어 있는 상황
        // (현재 데이터에는 없지만, 데이터 전제에 기대지 않고 조건이 지켜지는지 확인한다)
        given(stationRepository.findById(300L)).willReturn(Optional.of(station(300L, "서울역", false)));
        lenient().when(placeQueryService.getPlacesByStation(300L))
                .thenReturn(List.of(place(11L, "CULTURE", "문화공간")));

        // when
        stationQueryService.getStationPlaces(300L, null);

        // then: 장소 조회 자체를 하지 않고 카테고리·태그가 빈 채로 나간다
        verify(placeQueryService, never()).getPlacesByStation(any());
        verify(placeInfoQueryService, never()).getTopTagNames(any());
        ArgumentCaptor<List<StationPlaceCategoryResponse>> categoriesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> tagsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stationConverter).toPlacesResponse(any(), any(), tagsCaptor.capture(), any(), categoriesCaptor.capture());
        assertThat(categoriesCaptor.getValue()).isEmpty();
        assertThat(tagsCaptor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 역이면 예외가 발생한다")
    void getStationPlaces_stationNotFound() {
        // given
        given(stationRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> stationQueryService.getStationPlaces(999L, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(StationErrorCode.STATION_NOT_FOUND.getMessage());
        verify(placeQueryService, never()).getPlacesByStation(any());
    }

    // ---------- 역별 장소 목록: 맞춤추천 태그 우선 정렬 ----------

    private static final List<String> TRAVEL_STYLES = List.of("NATURE", "BUDGET", "EXPERIENCE");

    @Test
    @DisplayName("travelStyles가 없으면 태그 조회 없이 기존처럼 id순으로 고른다")
    void getStationPlaces_withoutTravelStyles_keepsIdOrder() {
        // given
        given(stationRepository.findById(6L)).willReturn(Optional.of(station(6L, "보문역")));
        given(placeQueryService.getPlacesByStation(6L)).willReturn(List.of(
                place(13L, "CULTURE", "문화공간"), place(11L, "CULTURE", "문화공간")));

        // when
        stationQueryService.getStationPlaces(6L, null);

        // then
        verify(placeInfoQueryService, never()).getTagNamesByPlace(any());
        ArgumentCaptor<List<PlaceInfoResponse>> placesCaptor = ArgumentCaptor.forClass(List.class);
        verify(stationConverter).toPlaceCategoryResponse(any(), any(), placesCaptor.capture());
        assertThat(placesCaptor.getValue()).extracting(PlaceInfoResponse::placeId).containsExactly(11L, 13L);
    }

    @Test
    @DisplayName("travelStyles가 있으면 카테고리 안에서 태그 매칭 개수가 많은 장소부터 우선 노출한다")
    void getStationPlaces_withTravelStyles_prioritizesTagMatches() {
        // given: 21은 2개 태그 일치, 22는 1개, 23은 0개
        given(stationRepository.findById(6L)).willReturn(Optional.of(station(6L, "보문역")));
        List<PlaceInfoResponse> places = List.of(
                place(23L, "CULTURE", "문화공간"), place(21L, "CULTURE", "문화공간"), place(22L, "CULTURE", "문화공간"));
        given(placeQueryService.getPlacesByStation(6L)).willReturn(places);
        given(placeInfoQueryService.getTagNamesByPlace(List.of(23L, 21L, 22L))).willReturn(Map.of(
                21L, List.of("NATURE", "BUDGET"),
                22L, List.of("NATURE"),
                23L, List.of("INDOOR")
        ));

        // when
        stationQueryService.getStationPlaces(6L, TRAVEL_STYLES);

        // then
        ArgumentCaptor<List<PlaceInfoResponse>> placesCaptor = ArgumentCaptor.forClass(List.class);
        verify(stationConverter).toPlaceCategoryResponse(any(), any(), placesCaptor.capture());
        assertThat(placesCaptor.getValue()).extracting(PlaceInfoResponse::placeId).containsExactly(21L, 22L, 23L);
    }

    @Test
    @DisplayName("태그 매칭 개수가 같은 장소끼리는 매번 같은 순서로 나오지 않는다")
    void getStationPlaces_tiesAreShuffled() {
        // given: 41~43 모두 매칭 0개. 3개가 정확히 3자리라 전부 뽑히지만 순서는 매번 달라야 한다
        given(stationRepository.findById(6L)).willReturn(Optional.of(station(6L, "보문역")));
        List<PlaceInfoResponse> places = List.of(
                place(41L, "CULTURE", "문화공간"), place(42L, "CULTURE", "문화공간"), place(43L, "CULTURE", "문화공간"));
        given(placeQueryService.getPlacesByStation(6L)).willReturn(places);
        given(placeInfoQueryService.getTagNamesByPlace(any())).willReturn(Map.of());

        // when: 20번 반복해 첫 번째 자리에 오는 id를 모은다
        for (int i = 0; i < 20; i++) {
            stationQueryService.getStationPlaces(6L, TRAVEL_STYLES);
        }
        ArgumentCaptor<List<PlaceInfoResponse>> captor = ArgumentCaptor.forClass(List.class);
        verify(stationConverter, times(20)).toPlaceCategoryResponse(any(), any(), captor.capture());
        java.util.Set<Long> firstIds = captor.getAllValues().stream()
                .map(selected -> selected.get(0).placeId())
                .collect(java.util.stream.Collectors.toSet());

        // then: 20번 시도해서 항상 같은 자리에만 오지는 않는다
        assertThat(firstIds).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("후보가 최대 개수보다 많으면 매칭 우선으로 3개만 남기고 매칭 0개끼리는 무작위로 채운다")
    void getStationPlaces_withTravelStyles_fillsRemainingRandomlyWhenNoMoreMatches() {
        // given: 31·32는 1개씩 매칭, 41~43은 매칭 0개 → 31,32는 항상 포함되고 세 번째 자리만 무작위
        given(stationRepository.findById(6L)).willReturn(Optional.of(station(6L, "보문역")));
        List<PlaceInfoResponse> places = List.of(
                place(31L, "CULTURE", "문화공간"), place(32L, "CULTURE", "문화공간"),
                place(41L, "CULTURE", "문화공간"), place(42L, "CULTURE", "문화공간"), place(43L, "CULTURE", "문화공간"));
        given(placeQueryService.getPlacesByStation(6L)).willReturn(places);
        given(placeInfoQueryService.getTagNamesByPlace(any())).willReturn(Map.of(
                31L, List.of("NATURE"),
                32L, List.of("BUDGET")
        ));

        // when
        ArgumentCaptor<List<PlaceInfoResponse>> captor = ArgumentCaptor.forClass(List.class);
        stationQueryService.getStationPlaces(6L, TRAVEL_STYLES);
        verify(stationConverter).toPlaceCategoryResponse(any(), any(), captor.capture());

        // then
        List<Long> selectedIds = captor.getValue().stream().map(PlaceInfoResponse::placeId).toList();
        assertThat(selectedIds).hasSize(3);
        assertThat(selectedIds).contains(31L, 32L);
        assertThat(selectedIds).anyMatch(id -> List.of(41L, 42L, 43L).contains(id));
    }

    @Test
    @DisplayName("존재하지 않는 여행 스타일 태그면 예외가 발생한다")
    void getStationPlaces_invalidTravelStyle() {
        // given
        given(stationRepository.findById(6L)).willReturn(Optional.of(station(6L, "보문역")));
        given(placeQueryService.getPlacesByStation(6L)).willReturn(List.of(place(11L, "CULTURE", "문화공간")));

        // when & then
        assertThatThrownBy(() -> stationQueryService.getStationPlaces(6L, List.of("NOT_A_TAG")))
                .isInstanceOf(CustomException.class);
    }
}
