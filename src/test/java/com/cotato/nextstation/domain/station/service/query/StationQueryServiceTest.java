package com.cotato.nextstation.domain.station.service.query;

import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.service.query.PlaceInfoQueryService;
import com.cotato.nextstation.domain.place.service.query.PlaceQueryService;
import com.cotato.nextstation.domain.station.converter.StationConverter;
import com.cotato.nextstation.domain.station.dto.response.StationPlaceCategoryResponse;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.repository.StationLineRepository.StationLineNameView;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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

    private StationLineNameView lineView(Long stationId, String lineName) {
        StationLineNameView view = mock(StationLineNameView.class);
        given(view.getStationId()).willReturn(stationId);
        given(view.getLineName()).willReturn(lineName);
        return view;
    }

    @Test
    @DisplayName("역명이 일치하면 소속 노선을 모두 묶어 요약을 반환한다")
    void searchByName_found() {
        Station station = mock(Station.class);
        given(station.getId()).willReturn(42L);
        StationLineNameView view2 = lineView(42L, "2호선");
        StationLineNameView view5 = lineView(42L, "5호선");
        given(stationRepository.findByStationName("왕십리역")).willReturn(Optional.of(station));
        given(stationLineRepository.findLineNamesByStationIdIn(List.of(42L)))
                .willReturn(List.of(view2, view5));

        StationSummaryResponse expected = new StationSummaryResponse(42L, "왕십리역", List.of("2호선", "5호선"));
        ArgumentCaptor<List<String>> linesCaptor = ArgumentCaptor.forClass(List.class);
        given(stationConverter.toSummaryResponse(eq(station), linesCaptor.capture())).willReturn(expected);

        // when
        List<StationSummaryResponse> result = stationQueryService.searchByName("왕십리역");

        // then
        assertThat(result).containsExactly(expected);
        assertThat(linesCaptor.getValue()).containsExactly("2호선", "5호선");
    }

    @Test
    @DisplayName("일치하는 역이 없으면 빈 목록을 반환한다")
    void searchByName_notFound() {
        // given
        given(stationRepository.findByStationName("없는역")).willReturn(Optional.empty());

        // when
        List<StationSummaryResponse> result = stationQueryService.searchByName("없는역");

        // then
        assertThat(result).isEmpty();
        verify(stationConverter, never()).toSummaryResponse(any(), any());
    }

    // ---------- 역별 장소 목록 ----------

    private Station station(Long id, String name) {
        Station station = Station.builder().stationName(name).isDrawable(true).build();
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
        stationQueryService.getStationPlaces(6L);

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
        stationQueryService.getStationPlaces(6L);

        // then
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(stationConverter).toPlacesResponse(eq(station), any(), any(), nameCaptor.capture(), any());
        assertThat(nameCaptor.getValue()).isEqualTo("보문역 환승여행 코스");
    }

    @Test
    @DisplayName("뽑기 대상이 아니라 장소가 없는 역은 빈 목록으로 응답한다")
    void getStationPlaces_noPlaces() {
        // given
        given(stationRepository.findById(300L)).willReturn(Optional.of(station(300L, "서울역")));
        given(placeQueryService.getPlacesByStation(300L)).willReturn(List.of());

        // when
        stationQueryService.getStationPlaces(300L);

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
        StationLineNameView line6 = lineView(6L, "6호선");
        StationLineNameView lineUi = lineView(6L, "우이신설선");
        given(stationRepository.findById(6L)).willReturn(Optional.of(station(6L, "보문역")));
        List<PlaceInfoResponse> places = List.of(place(11L, "CULTURE", "문화공간"), place(30L, "FOOD", "식당"));
        given(placeQueryService.getPlacesByStation(6L)).willReturn(places);
        given(stationLineRepository.findLineNamesByStationIdIn(List.of(6L)))
                .willReturn(List.of(line6, lineUi));
        given(placeInfoQueryService.getTopTagNames(List.of(11L, 30L)))
                .willReturn(List.of("LOCAL_EXPLORE", "NATURE", "BUDGET"));

        // when
        stationQueryService.getStationPlaces(6L);

        // then
        ArgumentCaptor<List<String>> linesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> tagsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stationConverter).toPlacesResponse(any(), linesCaptor.capture(), tagsCaptor.capture(), any(), any());
        assertThat(linesCaptor.getValue()).containsExactly("6호선", "우이신설선");
        assertThat(tagsCaptor.getValue()).containsExactly("LOCAL_EXPLORE", "NATURE", "BUDGET");
    }

    @Test
    @DisplayName("장소가 없으면 태그 조회를 호출하지 않고 빈 태그를 내려준다")
    void getStationPlaces_noPlacesSkipsTagLookup() {
        // given
        given(stationRepository.findById(300L)).willReturn(Optional.of(station(300L, "서울역")));
        given(placeQueryService.getPlacesByStation(300L)).willReturn(List.of());

        // when
        stationQueryService.getStationPlaces(300L);

        // then: 빈 id 목록으로 태그를 조회하는 낭비를 막는다
        verify(placeInfoQueryService, never()).getTopTagNames(any());
        ArgumentCaptor<List<String>> tagsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stationConverter).toPlacesResponse(any(), any(), tagsCaptor.capture(), any(), any());
        assertThat(tagsCaptor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 역이면 예외가 발생한다")
    void getStationPlaces_stationNotFound() {
        // given
        given(stationRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> stationQueryService.getStationPlaces(999L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(StationErrorCode.STATION_NOT_FOUND.getMessage());
        verify(placeQueryService, never()).getPlacesByStation(any());
    }
}
