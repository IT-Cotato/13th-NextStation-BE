package com.cotato.nextstation.domain.departure.service.command;

import com.cotato.nextstation.domain.departure.converter.DepartureStationConverter;
import com.cotato.nextstation.domain.departure.dto.request.DepartureStationCreateRequest;
import com.cotato.nextstation.domain.departure.entity.MemberDepartureStation;
import com.cotato.nextstation.domain.departure.exception.DepartureStationErrorCode;
import com.cotato.nextstation.domain.departure.repository.MemberDepartureStationRepository;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DepartureStationCommandServiceTest {

    @InjectMocks
    private DepartureStationCommandService departureStationCommandService;

    @Mock
    private MemberDepartureStationRepository memberDepartureStationRepository;

    @Mock
    private DepartureStationConverter departureStationConverter;

    @Test
    @DisplayName("출발역이 10개 미만이면 다음 순서로 저장된다")
    void addDepartureStation_success() {
        // given
        Long memberId = 1L;
        DepartureStationCreateRequest request = new DepartureStationCreateRequest(100L, "집");
        MemberDepartureStation entity = MemberDepartureStation.builder()
                .memberId(memberId).stationId(100L).label("집").orderNum(4).build();
        given(memberDepartureStationRepository.countByMemberId(memberId)).willReturn(3L);
        given(memberDepartureStationRepository.findMaxOrderNumByMemberId(memberId)).willReturn(3);
        given(departureStationConverter.toEntity(memberId, request, 4)).willReturn(entity);
        given(memberDepartureStationRepository.save(entity)).willReturn(entity);

        // when
        departureStationCommandService.addDepartureStation(memberId, request);

        // then
        verify(departureStationConverter).toEntity(memberId, request, 4);
        verify(memberDepartureStationRepository).save(entity);
    }

    @Test
    @DisplayName("출발역이 이미 10개면 예외가 발생하고 저장하지 않는다")
    void addDepartureStation_maxExceeded() {
        // given
        Long memberId = 1L;
        DepartureStationCreateRequest request = new DepartureStationCreateRequest(100L, "집");
        given(memberDepartureStationRepository.countByMemberId(memberId)).willReturn(10L);

        // when & then
        assertThatThrownBy(() -> departureStationCommandService.addDepartureStation(memberId, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(DepartureStationErrorCode.MAX_DEPARTURE_STATIONS_EXCEEDED.getMessage());
        verify(memberDepartureStationRepository, never()).save(any());
    }

    @Test
    @DisplayName("본인 소유 출발역은 삭제된다")
    void deleteDepartureStation_success() {
        // given
        Long memberId = 1L;
        Long departureStationId = 5L;
        MemberDepartureStation entity = MemberDepartureStation.builder()
                .memberId(memberId).stationId(100L).label("집").orderNum(1).build();
        given(memberDepartureStationRepository.findByIdAndMemberId(departureStationId, memberId))
                .willReturn(Optional.of(entity));

        // when
        departureStationCommandService.deleteDepartureStation(memberId, departureStationId);

        // then
        verify(memberDepartureStationRepository).delete(entity);
    }

    @Test
    @DisplayName("본인 소유가 아니거나 없는 출발역을 삭제하면 예외가 발생한다")
    void deleteDepartureStation_notFound() {
        // given
        Long memberId = 1L;
        Long departureStationId = 5L;
        given(memberDepartureStationRepository.findByIdAndMemberId(departureStationId, memberId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> departureStationCommandService.deleteDepartureStation(memberId, departureStationId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(DepartureStationErrorCode.DEPARTURE_STATION_NOT_FOUND.getMessage());
        verify(memberDepartureStationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("저장된 출발역이 없으면 첫 순서는 1이다")
    void addDepartureStation_firstOrderNum() {
        // given
        Long memberId = 1L;
        DepartureStationCreateRequest request = new DepartureStationCreateRequest(100L, null);
        MemberDepartureStation entity = MemberDepartureStation.builder()
                .memberId(memberId).stationId(100L).orderNum(1).build();
        given(memberDepartureStationRepository.countByMemberId(memberId)).willReturn(0L);
        given(memberDepartureStationRepository.findMaxOrderNumByMemberId(memberId)).willReturn(0);
        given(departureStationConverter.toEntity(memberId, request, 1)).willReturn(entity);
        given(memberDepartureStationRepository.save(entity)).willReturn(entity);

        // when
        departureStationCommandService.addDepartureStation(memberId, request);

        // then
        verify(departureStationConverter).toEntity(memberId, request, 1);
    }
}
