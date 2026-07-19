package com.cotato.nextstation.domain.departure.service.query;

import com.cotato.nextstation.domain.departure.converter.DepartureStationConverter;
import com.cotato.nextstation.domain.departure.dto.response.DepartureStationResponse;
import com.cotato.nextstation.domain.departure.entity.MemberDepartureStation;
import com.cotato.nextstation.domain.departure.repository.MemberDepartureStationRepository;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.service.query.StationQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DepartureStationQueryServiceTest {

    @InjectMocks
    private DepartureStationQueryService departureStationQueryService;

    @Mock
    private MemberDepartureStationRepository memberDepartureStationRepository;
    @Mock
    private StationQueryService stationQueryService;
    @Mock
    private DepartureStationConverter departureStationConverter;

    private MemberDepartureStation departureStation(Long stationId, int orderNum) {
        return MemberDepartureStation.builder().memberId(1L).stationId(stationId).orderNum(orderNum).build();
    }

    @Test
    @DisplayName("저장된 stationId들로 역 요약을 조회해 응답을 만든다")
    void getDepartureStations_enrichesWithStation() {
        // given
        List<MemberDepartureStation> stored = List.of(departureStation(100L, 1), departureStation(200L, 2));
        Map<Long, StationSummaryResponse> summaries = Map.of(
                100L, new StationSummaryResponse(100L, "왕십리역", List.of("2호선")),
                200L, new StationSummaryResponse(200L, "강남역", List.of("2호선", "신분당선")));
        List<DepartureStationResponse> expected = List.of(
                new DepartureStationResponse(1L, 100L, "왕십리역", List.of("2호선"), 1, null));
        given(memberDepartureStationRepository.findByMemberIdOrderByOrderNumAsc(1L)).willReturn(stored);
        given(stationQueryService.getSummariesByStationIds(List.of(100L, 200L))).willReturn(summaries);
        given(departureStationConverter.toResponses(stored, summaries)).willReturn(expected);

        // when
        List<DepartureStationResponse> result = departureStationQueryService.getDepartureStations(1L);

        // then
        assertThat(result).isEqualTo(expected);
        verify(stationQueryService).getSummariesByStationIds(eq(List.of(100L, 200L)));
    }
}
