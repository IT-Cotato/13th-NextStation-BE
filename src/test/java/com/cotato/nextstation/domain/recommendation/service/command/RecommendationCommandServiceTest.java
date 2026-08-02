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
    @DisplayName("태그 점수 상위 5개 역만 후보로 남고, 그보다 낮은 점수의 역은 후보에서 빠진다")
    void recommendCustom_limitsToTopFiveByScore() {
        // given: 점수는 station6(60) > 5(50) > 4(40) > 3(30) > 2(20) > 1(10) 순. 방문 이력은 없다.
        // 상위 5개(2~6)만 후보군에 남고 station1은 잘려야 한다.
        givenDeparture(1L);
        List<ReachableStationView> routes = java.util.stream.LongStream.rangeClosed(1, 6)
                .mapToObj(id -> reachableView(id, 10))
                .toList();
        given(stationRouteRepository.findAllFromDeparture(1L)).willReturn(routes);
        given(stationRepository.findAllById(any())).willReturn(java.util.stream.LongStream.rangeClosed(1, 6)
                .mapToObj(id -> station(id, id + "역"))
                .toList());
        Map<Long, Map<String, Long>> counts = new java.util.HashMap<>();
        for (long id = 1; id <= 6; id++) {
            counts.put(id, Map.of("NATURE", id * 10L));
        }
        given(stationTagCountReader.getPlaceCountsByStationForTags(TRAVEL_STYLES)).willReturn(counts);

        // when: 반복 추천해 나온 역 id를 모은다
        java.util.Set<Long> pickedIds = new java.util.HashSet<>();
        for (int i = 0; i < 30; i++) {
            CustomRecommendationResponse response = recommendationCommandService.recommendCustom(1L,
                    customRequest(1L, TravelTime.ANY, TRAVEL_STYLES));
            pickedIds.add(response.station().stationId());
        }

        // then: 최하위 station1은 한 번도 나오지 않고, 상위 5개(2~6) 안에서는 매번 같은 역만 나오지 않는다
        assertThat(pickedIds).doesNotContain(1L);
        assertThat(pickedIds).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("가본 역은 감점되어 컷 경계에서 밀려날 수 있다")
    void recommendCustom_visitedStationIsPenalizedAtCutBoundary() {
        // given: 감점 전 점수는 station1(60)>2(50)>3(40)>4(30)>5(22)>6(20) 순 — 상위 5개는 1~5, station6은 컷 밖.
        // station5(가본 역, 점수 22)는 감점(4점)되면 18이 돼 station6(20)보다 낮아져 컷 경계에서 역전된다.
        // 즉 감점 없이는 station5가 후보에 남고, 감점이 제대로 반영되면 station5는 빠지고 station6이 그 자리를 채운다.
        givenDeparture(1L);
        List<ReachableStationView> routes = java.util.stream.LongStream.rangeClosed(1, 6)
                .mapToObj(id -> reachableView(id, 10))
                .toList();
        given(stationRouteRepository.findAllFromDeparture(1L)).willReturn(routes);
        given(stationRepository.findAllById(any())).willReturn(java.util.stream.LongStream.rangeClosed(1, 6)
                .mapToObj(id -> station(id, id + "역"))
                .toList());
        Map<Long, Map<String, Long>> counts = new java.util.HashMap<>();
        counts.put(1L, Map.of("NATURE", 50L));
        counts.put(2L, Map.of("NATURE", 40L));
        counts.put(3L, Map.of("NATURE", 30L));
        counts.put(4L, Map.of("NATURE", 20L));
        counts.put(5L, Map.of("NATURE", 12L));
        counts.put(6L, Map.of("NATURE", 10L));
        given(stationTagCountReader.getPlaceCountsByStationForTags(TRAVEL_STYLES)).willReturn(counts);
        given(courseRepository.findVisitedStationIds(1L)).willReturn(List.of(5L));

        // when
        java.util.Set<Long> pickedIds = new java.util.HashSet<>();
        for (int i = 0; i < 30; i++) {
            CustomRecommendationResponse response = recommendationCommandService.recommendCustom(1L,
                    customRequest(1L, TravelTime.ANY, TRAVEL_STYLES));
            pickedIds.add(response.station().stationId());
        }

        // then: 감점으로 컷 밖으로 밀려난 station5는 한 번도 나오지 않는다
        assertThat(pickedIds).doesNotContain(5L);
    }

    @Test
    @DisplayName("후보가 가본 역뿐이어도 감점만 받을 뿐 추천에서 제외되지 않는다")
    void recommendCustom_visitedOnlyStationIsStillRecommended() {
        // given: 도달 가능한 역이 station1 하나뿐이고 이미 가본 역이다.
        givenDeparture(1L);
        List<ReachableStationView> routes = List.of(reachableView(1L, 10));
        given(stationRouteRepository.findAllFromDeparture(1L)).willReturn(routes);
        given(stationRepository.findAllById(any())).willReturn(List.of(station(1L, "A역")));
        given(stationTagCountReader.getPlaceCountsByStationForTags(TRAVEL_STYLES)).willReturn(Map.of());
        given(courseRepository.findVisitedStationIds(1L)).willReturn(List.of(1L));

        // when
        CustomRecommendationResponse response = recommendationCommandService.recommendCustom(1L,
                customRequest(1L, TravelTime.ANY, TRAVEL_STYLES));

        // then: 감점되어도 유일한 후보이므로 그대로 추천된다
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
