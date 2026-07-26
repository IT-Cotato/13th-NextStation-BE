package com.cotato.nextstation.domain.recommendation.service.command;

import com.cotato.nextstation.domain.recommendation.converter.RecommendationConverter;
import com.cotato.nextstation.domain.recommendation.dto.response.RandomRecommendationResponse;
import com.cotato.nextstation.domain.recommendation.entity.RecommendationLog;
import com.cotato.nextstation.domain.recommendation.exception.RecommendationErrorCode;
import com.cotato.nextstation.domain.recommendation.repository.RecommendationLogRepository;
import com.cotato.nextstation.domain.recommendation.service.port.StationPlaceReader;
import com.cotato.nextstation.domain.recommendation.service.port.StationPlaceView;
import com.cotato.nextstation.domain.station.entity.Line;
import com.cotato.nextstation.domain.station.entity.LineCode;
import com.cotato.nextstation.domain.station.entity.Station;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
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

    private final RecommendationConverter recommendationConverter = new RecommendationConverter(new LineConverter());

    private RecommendationCommandService recommendationCommandService;

    @BeforeEach
    void setUp() {
        recommendationCommandService = new RecommendationCommandService(
                stationRepository, recommendationLogRepository, stationPlaceReader, recommendationConverter);
    }

    private Station station(Long id, String name) {
        Station station = Station.builder()
                .stationName(name).description(name + " 소개").todo(name + " 할일").isDrawable(true).build();
        ReflectionTestUtils.setField(station, "id", id);
        return station;
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
        ReflectionTestUtils.setField(station, "drawLine", Line.of(LineCode.LINE_1));
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
        assertThat(response.station().line().name()).isEqualTo("1호선");
        assertThat(response.station().description()).isEqualTo("제기동역 소개");
        assertThat(response.station().todo()).isEqualTo("제기동역 할일");
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
}
