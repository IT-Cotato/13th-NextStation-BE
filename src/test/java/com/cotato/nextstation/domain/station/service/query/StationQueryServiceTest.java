package com.cotato.nextstation.domain.station.service.query;

import com.cotato.nextstation.domain.station.converter.StationConverter;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.repository.StationLineRepository.StationLineNameView;
import com.cotato.nextstation.domain.station.repository.StationLineRepository;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
}
