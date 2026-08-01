package com.cotato.nextstation.domain.recommendation.service.command;

import com.cotato.nextstation.domain.course.repository.CourseRepository;
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
import com.cotato.nextstation.domain.route.repository.StationRouteRepository.ReachableStationView;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.Line;
import com.cotato.nextstation.domain.station.entity.LineCode;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.repository.StationLineRepository;
import com.cotato.nextstation.domain.station.repository.StationLineRepository.StationLineView;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import com.cotato.nextstation.domain.station.converter.LineConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecommendationCommandServiceTest {

    @Mock
    private StationRepository stationRepository;

    @Mock
    private RecommendationLogRepository recommendationLogRepository;

    @Mock
    private StationPlaceReader stationPlaceReader;

    @Mock
    private StationLineRepository stationLineRepository;

    @Mock
    private StationRouteRepository stationRouteRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StationTagCountReader stationTagCountReader;

    private final RecommendationConverter recommendationConverter = new RecommendationConverter(new LineConverter());

    private RecommendationCommandService recommendationCommandService;

    @BeforeEach
    void setUp() {
        recommendationCommandService = new RecommendationCommandService(
                stationRepository, stationLineRepository, stationRouteRepository, courseRepository,
                recommendationLogRepository, stationPlaceReader, stationTagCountReader, recommendationConverter);
        lenient().when(stationLineRepository.findLinesByStationIdIn(any())).thenReturn(List.of());
        lenient().when(courseRepository.findVisitedStationIds(any())).thenReturn(List.of());
    }

    private Station station(Long id, String name) {
        Station station = Station.builder()
                .stationName(name).description(name + " 소개").todo(name + " 할일").isDrawable(true).build();
        ReflectionTestUtils.setField(station, "id", id);
        return station;
    }

    private Line line(Long id, LineCode code) {
        Line line = Line.of(code);
        ReflectionTestUtils.setField(line, "id", id);
        return line;
    }

    private StationLineView lineView(Long stationId, Long lineId, LineCode code) {
        StationLineView view = mock(StationLineView.class);
        lenient().when(view.getStationId()).thenReturn(stationId);
        lenient().when(view.getLineId()).thenReturn(lineId);
        lenient().when(view.getLineName()).thenReturn(code.getDisplayName());
        lenient().when(view.getLineCode()).thenReturn(code);
        return view;
    }

    private StationPlaceView place(Long id, String categoryCode) {
        return new StationPlaceView(id, "장소" + id, "설명", categoryCode, categoryCode, "img", 127.0, 37.5);
    }

    @Test
    @DisplayName("로그인 사용자는 직전 추천 역이 후보에서 제외되고 로그가 기록된다")
    void drawRandom_excludesLastRecommended() {
        // given
        Long memberId = 1L;
        given(stationRepository.findByIsDrawableTrue()).willReturn(List.of(station(1L, "A역"), station(2L, "B역")));
        given(recommendationLogRepository.findTopByMemberIdOrderByCreatedAtDescIdDesc(memberId))
                .willReturn(Optional.of(RecommendationLog.builder().memberId(memberId).resultStationId(1L).isRandom(true).build()));
        given(stationPlaceReader.getPlacesByStation(anyLong())).willReturn(List.of());

        // when
        RandomRecommendationResponse response = recommendationCommandService.drawRandom(memberId);

        // then
        assertThat(response.station().stationId()).isEqualTo(2L);
        ArgumentCaptor<RecommendationLog> captor = ArgumentCaptor.forClass(RecommendationLog.class);
        verify(recommendationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getResultStationId()).isEqualTo(2L);
        assertThat(captor.getValue().getMemberId()).isEqualTo(memberId);
        assertThat(captor.getValue().isRandom()).isTrue();
    }

    @Test
    @DisplayName("뽑기 역이 직전 추천 1개뿐이면 제외하지 않고 그 역을 다시 뽑는다(dead-end 방지)")
    void drawRandom_deadEndFallsBackToAll() {
        // given
        Long memberId = 1L;
        given(stationRepository.findByIsDrawableTrue()).willReturn(List.of(station(1L, "A역")));
        given(recommendationLogRepository.findTopByMemberIdOrderByCreatedAtDescIdDesc(memberId))
                .willReturn(Optional.of(RecommendationLog.builder().memberId(memberId).resultStationId(1L).isRandom(true).build()));
        given(stationPlaceReader.getPlacesByStation(anyLong())).willReturn(List.of());

        // when
        RandomRecommendationResponse response = recommendationCommandService.drawRandom(memberId);

        // then
        assertThat(response.station().stationId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("비로그인 뽑기는 직전 추천을 조회하지 않고 memberId 없이 로그를 남긴다")
    void drawRandom_anonymousSkipsExclusion() {
        // given
        given(stationRepository.findByIsDrawableTrue()).willReturn(List.of(station(1L, "A역")));
        given(stationPlaceReader.getPlacesByStation(anyLong())).willReturn(List.of());

        // when
        RandomRecommendationResponse response = recommendationCommandService.drawRandom(null);

        // then
        assertThat(response.station().stationId()).isEqualTo(1L);
        verify(recommendationLogRepository, never()).findTopByMemberIdOrderByCreatedAtDescIdDesc(any());
        ArgumentCaptor<RecommendationLog> captor = ArgumentCaptor.forClass(RecommendationLog.class);
        verify(recommendationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getMemberId()).isNull();
    }

    @Test
    @DisplayName("뽑기 대상 역이 없으면 예외가 발생하고 로그를 남기지 않는다")
    void drawRandom_noDrawableStation() {
        // given
        given(stationRepository.findByIsDrawableTrue()).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> recommendationCommandService.drawRandom(null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(RecommendationErrorCode.NO_DRAWABLE_STATION.getMessage());
        verify(recommendationLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("코스 미리보기는 카테고리 순서(문화/식당/카페/산책)대로 카테고리당 1개씩 구성되고 없는 카테고리는 제외된다")
    void drawRandom_coursePreviewSelectsOnePerCategory() {
        // given
        Station station = station(10L, "제기동역");
        given(stationRepository.findByIsDrawableTrue()).willReturn(List.of(station));
        // 순서 섞고 FOOD 2개, WALK 없음
        given(stationPlaceReader.getPlacesByStation(10L)).willReturn(List.of(
                place(100L, "FOOD"),
                place(200L, "CULTURE"),
                place(300L, "FOOD"),
                place(400L, "CAFE")
        ));

        // when
        RandomRecommendationResponse response = recommendationCommandService.drawRandom(null);

        // then
        assertThat(response.station().description()).isEqualTo("제기동역 소개");
        assertThat(response.course().name()).isEqualTo("제기동역 환승여행 코스");
        assertThat(response.course().places())
                .extracting(p -> p.categoryCode())
                .containsExactly("CULTURE", "FOOD", "CAFE");
        // FOOD가 2개면 그중 하나가 무작위로 선택된다
        assertThat(response.course().places().get(1).placeId()).isIn(100L, 300L);
    }

    @Test
    @DisplayName("같은 역이라도 카테고리 안에서는 무작위로 골라 매번 같은 코스만 나오지는 않는다")
    void drawRandom_coursePreviewPicksRandomlyWithinCategory() {
        // given: FOOD 후보가 20개인 역을 반복해서 뽑는다
        Station station = station(10L, "제기동역");
        given(stationRepository.findByIsDrawableTrue()).willReturn(List.of(station));
        List<StationPlaceView> foodPlaces = java.util.stream.LongStream.rangeClosed(1, 20)
                .mapToObj(id -> place(id, "FOOD"))
                .toList();
        given(stationPlaceReader.getPlacesByStation(10L)).willReturn(foodPlaces);

        // when: 30번 뽑아 선택된 장소 id를 모은다
        java.util.Set<Long> pickedIds = new java.util.HashSet<>();
        for (int i = 0; i < 30; i++) {
            pickedIds.add(recommendationCommandService.drawRandom(null)
                    .course().places().get(0).placeId());
        }

        // then: 항상 같은 장소만 나오지는 않는다 (후보 20개 중 30회 시도)
        assertThat(pickedIds).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("환승역은 소속 노선이 모두 내려가고 대표 노선이 맨 앞에 온다")
    void drawRandom_returnsAllLinesWithDrawLineFirst() {
        // given: 6호선·우이신설선 환승역이고 대표 노선은 우이신설선
        Station station = station(10L, "보문역");
        ReflectionTestUtils.setField(station, "drawLine", line(21L, LineCode.UI_SINSEOL));
        List<StationLineView> lineViews = List.of(
                lineView(10L, 6L, LineCode.LINE_6),
                lineView(10L, 21L, LineCode.UI_SINSEOL)
        );
        given(stationRepository.findByIsDrawableTrue()).willReturn(List.of(station));
        given(stationLineRepository.findLinesByStationIdIn(List.of(10L))).willReturn(lineViews);
        given(stationPlaceReader.getPlacesByStation(anyLong())).willReturn(List.of());

        // when
        RandomRecommendationResponse response = recommendationCommandService.drawRandom(null);

        // then
        assertThat(response.station().lines())
                .extracting(LineSummaryResponse::name)
                .containsExactly("우이신설선", "6호선");
    }

    @Test
    @DisplayName("대표 노선이 없으면 소속 노선을 조회 순서(노선 ID 순) 그대로 내려준다")
    void drawRandom_keepsLineOrderWithoutDrawLine() {
        // given
        Station station = station(10L, "보문역");
        List<StationLineView> lineViews = List.of(
                lineView(10L, 6L, LineCode.LINE_6),
                lineView(10L, 21L, LineCode.UI_SINSEOL)
        );
        given(stationRepository.findByIsDrawableTrue()).willReturn(List.of(station));
        given(stationLineRepository.findLinesByStationIdIn(List.of(10L))).willReturn(lineViews);
        given(stationPlaceReader.getPlacesByStation(anyLong())).willReturn(List.of());

        // when
        RandomRecommendationResponse response = recommendationCommandService.drawRandom(null);

        // then
        assertThat(response.station().lines())
                .extracting(LineSummaryResponse::name)
                .containsExactly("6호선", "우이신설선");
    }

    @Test
    @DisplayName("역 할 일은 줄 단위로 나뉘고 번호 접두사는 제거된다")
    void drawRandom_splitsTodoIntoItems() {
        // given
        Station station = stationWithTodo(10L, "보문역",
                "1. 성북천을 따라 가볍게 산책하기\n2. 보문동 골목과 생활 상권 둘러보기\n\n3) 대학가 카페 들르기");
        given(stationRepository.findByIsDrawableTrue()).willReturn(List.of(station));
        given(stationPlaceReader.getPlacesByStation(anyLong())).willReturn(List.of());

        // when
        RandomRecommendationResponse response = recommendationCommandService.drawRandom(null);

        // then: 빈 줄은 버리고 "1. " 같은 번호는 떼어낸다
        assertThat(response.station().todos()).containsExactly(
                "성북천을 따라 가볍게 산책하기",
                "보문동 골목과 생활 상권 둘러보기",
                "대학가 카페 들르기"
        );
    }

    @Test
    @DisplayName("역 할 일이 비어 있으면 빈 목록을 내려준다")
    void drawRandom_emptyTodo() {
        // given
        Station station = stationWithTodo(10L, "보문역", null);
        given(stationRepository.findByIsDrawableTrue()).willReturn(List.of(station));
        given(stationPlaceReader.getPlacesByStation(anyLong())).willReturn(List.of());

        // when
        RandomRecommendationResponse response = recommendationCommandService.drawRandom(null);

        // then
        assertThat(response.station().todos()).isEmpty();
    }

    private Station stationWithTodo(Long id, String name, String todo) {
        Station station = Station.builder()
                .stationName(name).description(name + " 소개").todo(todo).isDrawable(true).build();
        ReflectionTestUtils.setField(station, "id", id);
        return station;
    }

    // ---------- 맞춤추천 ----------

    private static final List<String> TRAVEL_STYLES = List.of("NATURE", "BUDGET", "EXPERIENCE");

    private ReachableStationView reachableView(Long stationId, int durationMinutes) {
        ReachableStationView view = mock(ReachableStationView.class);
        lenient().when(view.getStationId()).thenReturn(stationId);
        lenient().when(view.getDurationMinutes()).thenReturn(durationMinutes);
        return view;
    }

    private CustomRecommendationRequest customRequest(Long departureStationId, TravelTime travelTime, List<String> travelStyles) {
        return new CustomRecommendationRequest(departureStationId, travelTime, travelStyles);
    }

    // 출발역은 항상 존재하는 것으로 스텁하고, 도달 가능 구간은 '상관없음'으로 단순화한다.
    private void givenDeparture(Long departureStationId) {
        given(stationRepository.existsById(departureStationId)).willReturn(true);
    }

    @Test
    @DisplayName("존재하지 않는 출발역이면 예외가 발생한다")
    void recommendCustom_departureStationNotFound() {
        // given
        given(stationRepository.existsById(99L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> recommendationCommandService.recommendCustom(1L,
                customRequest(99L, TravelTime.ANY, TRAVEL_STYLES)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(RecommendationErrorCode.DEPARTURE_STATION_NOT_FOUND.getMessage());
        verify(recommendationLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("여행 스타일이 중복되면 예외가 발생한다")
    void recommendCustom_duplicateTravelStyle() {
        // given
        givenDeparture(1L);

        // when & then
        assertThatThrownBy(() -> recommendationCommandService.recommendCustom(1L,
                customRequest(1L, TravelTime.ANY, List.of("NATURE", "NATURE", "BUDGET"))))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(RecommendationErrorCode.DUPLICATE_TRAVEL_STYLE.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 여행 스타일이면 예외가 발생한다")
    void recommendCustom_invalidTravelStyle() {
        // given
        givenDeparture(1L);

        // when & then
        assertThatThrownBy(() -> recommendationCommandService.recommendCustom(1L,
                customRequest(1L, TravelTime.ANY, List.of("NATURE", "BUDGET", "NOT_A_TAG"))))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(RecommendationErrorCode.INVALID_TRAVEL_STYLE.getMessage());
    }

    @Test
    @DisplayName("도달 가능한 역이 없으면 예외가 발생한다")
    void recommendCustom_noReachableStation() {
        // given
        givenDeparture(1L);
        given(stationRouteRepository.findAllFromDeparture(1L)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> recommendationCommandService.recommendCustom(1L,
                customRequest(1L, TravelTime.ANY, TRAVEL_STYLES)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(RecommendationErrorCode.NO_REACHABLE_STATION.getMessage());
        verify(recommendationLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("이동 가능 시간이 정해져 있으면 그 시간 이내 구간만 조회한다")
    void recommendCustom_usesReachableWithLimit() {
        // given
        givenDeparture(1L);
        List<ReachableStationView> routes = List.of(reachableView(2L, 20));
        given(stationRouteRepository.findReachable(1L, 30)).willReturn(routes);
        given(stationRepository.findAllById(any())).willReturn(List.of(station(2L, "B역")));
        given(stationTagCountReader.getPlaceCountsByStationForTags(TRAVEL_STYLES)).willReturn(Map.of());

        // when
        recommendationCommandService.recommendCustom(1L, customRequest(1L, TravelTime.THIRTY_MINUTES, TRAVEL_STYLES));

        // then
        verify(stationRouteRepository).findReachable(1L, 30);
        verify(stationRouteRepository, never()).findAllFromDeparture(any());
    }

    @Test
    @DisplayName("태그 점수가 최고점의 90% 미만인 역은 후보에서 빠진다(가중치 10)")
    void recommendCustom_filtersByScoreRatio() {
        // given: A는 1개 태그만 겹쳐 점수 11(=1+10), B는 3개 태그가 모두 겹쳐 점수 45(=15+30)
        // 임계값 = 45*0.9 = 40.5 → A는 탈락하고 B만 남아 결과가 B로 고정된다
        givenDeparture(1L);
        List<ReachableStationView> routes = List.of(reachableView(1L, 10), reachableView(2L, 20));
        given(stationRouteRepository.findAllFromDeparture(1L)).willReturn(routes);
        given(stationRepository.findAllById(any())).willReturn(List.of(station(1L, "A역"), station(2L, "B역")));
        given(stationTagCountReader.getPlaceCountsByStationForTags(TRAVEL_STYLES)).willReturn(Map.of(
                1L, Map.of("NATURE", 1L),
                2L, Map.of("NATURE", 5L, "BUDGET", 5L, "EXPERIENCE", 5L)
        ));

        // when
        CustomRecommendationResponse response = recommendationCommandService.recommendCustom(1L,
                customRequest(1L, TravelTime.ANY, TRAVEL_STYLES));

        // then
        assertThat(response.station().stationId()).isEqualTo(2L);
        assertThat(response.travelDurationMinutes()).isEqualTo(20);
        ArgumentCaptor<RecommendationLog> captor = ArgumentCaptor.forClass(RecommendationLog.class);
        verify(recommendationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getResultStationId()).isEqualTo(2L);
        assertThat(captor.getValue().isRandom()).isFalse();
    }

    @Test
    @DisplayName("동점 후보 중 안 가본 역이 있으면 가본 역은 제외된다")
    void recommendCustom_excludesVisitedStation() {
        // given: A·B 동점 후보, A는 이미 가본 역
        givenDeparture(1L);
        List<ReachableStationView> routes = List.of(reachableView(1L, 10), reachableView(2L, 10));
        given(stationRouteRepository.findAllFromDeparture(1L)).willReturn(routes);
        given(stationRepository.findAllById(any())).willReturn(List.of(station(1L, "A역"), station(2L, "B역")));
        given(stationTagCountReader.getPlaceCountsByStationForTags(TRAVEL_STYLES)).willReturn(Map.of());
        given(courseRepository.findVisitedStationIds(1L)).willReturn(List.of(1L));

        // when
        CustomRecommendationResponse response = recommendationCommandService.recommendCustom(1L,
                customRequest(1L, TravelTime.ANY, TRAVEL_STYLES));

        // then
        assertThat(response.station().stationId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("후보 전부 가본 역이면 안 가본 역 조건은 적용하지 않는다")
    void recommendCustom_allVisitedFallsBackToAll() {
        // given
        givenDeparture(1L);
        List<ReachableStationView> routes = List.of(reachableView(1L, 10));
        given(stationRouteRepository.findAllFromDeparture(1L)).willReturn(routes);
        given(stationRepository.findAllById(any())).willReturn(List.of(station(1L, "A역")));
        given(stationTagCountReader.getPlaceCountsByStationForTags(TRAVEL_STYLES)).willReturn(Map.of());
        given(courseRepository.findVisitedStationIds(1L)).willReturn(List.of(1L));

        // when
        CustomRecommendationResponse response = recommendationCommandService.recommendCustom(1L,
                customRequest(1L, TravelTime.ANY, TRAVEL_STYLES));

        // then: 후보가 A뿐이라 결국 A가 나온다
        assertThat(response.station().stationId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("동점 후보 중 직전 추천 역은 제외된다")
    void recommendCustom_excludesLastRecommended() {
        // given
        givenDeparture(1L);
        List<ReachableStationView> routes = List.of(reachableView(1L, 10), reachableView(2L, 10));
        given(stationRouteRepository.findAllFromDeparture(1L)).willReturn(routes);
        given(stationRepository.findAllById(any())).willReturn(List.of(station(1L, "A역"), station(2L, "B역")));
        given(stationTagCountReader.getPlaceCountsByStationForTags(TRAVEL_STYLES)).willReturn(Map.of());
        given(recommendationLogRepository.findTopByMemberIdOrderByCreatedAtDescIdDesc(1L))
                .willReturn(Optional.of(RecommendationLog.builder().memberId(1L).resultStationId(1L).isRandom(false).build()));

        // when
        CustomRecommendationResponse response = recommendationCommandService.recommendCustom(1L,
                customRequest(1L, TravelTime.ANY, TRAVEL_STYLES));

        // then
        assertThat(response.station().stationId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("후보가 직전 추천 역 하나뿐이면 제외하지 않고 그 역을 다시 추천한다")
    void recommendCustom_lastRecommendedDeadEndFallsBack() {
        // given
        givenDeparture(1L);
        List<ReachableStationView> routes = List.of(reachableView(1L, 10));
        given(stationRouteRepository.findAllFromDeparture(1L)).willReturn(routes);
        given(stationRepository.findAllById(any())).willReturn(List.of(station(1L, "A역")));
        given(stationTagCountReader.getPlaceCountsByStationForTags(TRAVEL_STYLES)).willReturn(Map.of());
        given(recommendationLogRepository.findTopByMemberIdOrderByCreatedAtDescIdDesc(1L))
                .willReturn(Optional.of(RecommendationLog.builder().memberId(1L).resultStationId(1L).isRandom(false).build()));

        // when
        CustomRecommendationResponse response = recommendationCommandService.recommendCustom(1L,
                customRequest(1L, TravelTime.ANY, TRAVEL_STYLES));

        // then
        assertThat(response.station().stationId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("도달 가능해도 뽑기 대상이 아닌 역은 후보에서 빠진다")
    void recommendCustom_excludesNonDrawableReachableStation() {
        // given
        givenDeparture(1L);
        List<ReachableStationView> routes = List.of(reachableView(1L, 10));
        given(stationRouteRepository.findAllFromDeparture(1L)).willReturn(routes);
        Station notDrawable = Station.builder().stationName("출발전용역").isDrawable(false).build();
        ReflectionTestUtils.setField(notDrawable, "id", 1L);
        given(stationRepository.findAllById(any())).willReturn(List.of(notDrawable));

        // when & then
        assertThatThrownBy(() -> recommendationCommandService.recommendCustom(1L,
                customRequest(1L, TravelTime.ANY, TRAVEL_STYLES)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(RecommendationErrorCode.NO_REACHABLE_STATION.getMessage());
    }
}
