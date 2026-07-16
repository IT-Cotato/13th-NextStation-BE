package com.cotato.nextstation.domain.departure.service;

import com.cotato.nextstation.domain.departure.converter.DepartureStationConverter;
import com.cotato.nextstation.domain.departure.dto.request.DepartureStationCreateRequest;
import com.cotato.nextstation.domain.departure.dto.response.DepartureStationResponse;
import com.cotato.nextstation.domain.departure.entity.MemberDepartureStation;
import com.cotato.nextstation.domain.departure.exception.DepartureStationErrorCode;
import com.cotato.nextstation.domain.departure.repository.MemberDepartureStationRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartureStationCommandService {

    private static final int MAX_DEPARTURE_STATIONS = 10;

    private final MemberDepartureStationRepository memberDepartureStationRepository;
    private final DepartureStationConverter departureStationConverter;

    public DepartureStationResponse addDepartureStation(Long memberId, DepartureStationCreateRequest request) {
        if (memberDepartureStationRepository.countByMemberId(memberId) >= MAX_DEPARTURE_STATIONS) {
            throw new CustomException(DepartureStationErrorCode.MAX_DEPARTURE_STATIONS_EXCEEDED);
        }

        int nextOrderNum = memberDepartureStationRepository.findMaxOrderNumByMemberId(memberId) + 1;
        MemberDepartureStation saved = memberDepartureStationRepository.save(
                departureStationConverter.toEntity(memberId, request, nextOrderNum));
        return departureStationConverter.toResponse(saved);
    }

    public void deleteDepartureStation(Long memberId, Long departureStationId) {
        MemberDepartureStation departureStation = memberDepartureStationRepository
                .findByIdAndMemberId(departureStationId, memberId)
                .orElseThrow(() -> new CustomException(DepartureStationErrorCode.DEPARTURE_STATION_NOT_FOUND));
        memberDepartureStationRepository.delete(departureStation);
    }
}
