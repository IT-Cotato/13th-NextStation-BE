package com.cotato.nextstation.domain.departure.service.command;

import com.cotato.nextstation.domain.departure.converter.DepartureStationConverter;
import com.cotato.nextstation.domain.departure.dto.request.DepartureStationCreateRequest;
import com.cotato.nextstation.domain.departure.dto.response.DepartureStationCreateResponse;
import com.cotato.nextstation.domain.departure.entity.MemberDepartureStation;
import com.cotato.nextstation.domain.departure.exception.DepartureStationErrorCode;
import com.cotato.nextstation.domain.departure.repository.MemberDepartureStationRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartureStationCommandService {

    private static final int MAX_DEPARTURE_STATIONS = 10;

    private final MemberDepartureStationRepository memberDepartureStationRepository;
    private final DepartureStationConverter departureStationConverter;

    // 추가 응답에는 역명/노선을 싣지 않는다. 클라이언트가 역 검색에서 이미 알고 있고,
    // 목록 조회(GET)에서 역 정보를 채워 내려주므로 여기서 역을 다시 조회하지 않는다.
    public DepartureStationCreateResponse addDepartureStation(Long memberId, DepartureStationCreateRequest request) {
        if (memberDepartureStationRepository.countByMemberId(memberId) >= MAX_DEPARTURE_STATIONS) {
            throw new CustomException(DepartureStationErrorCode.MAX_DEPARTURE_STATIONS_EXCEEDED);
        }
        if (memberDepartureStationRepository.existsByMemberIdAndStationId(memberId, request.stationId())) {
            throw new CustomException(DepartureStationErrorCode.DUPLICATE_DEPARTURE_STATION);
        }

        int nextOrderNum = memberDepartureStationRepository.findMaxOrderNumByMemberId(memberId) + 1;
        try {
            MemberDepartureStation saved = memberDepartureStationRepository.save(
                    departureStationConverter.toEntity(memberId, request, nextOrderNum));
            return departureStationConverter.toCreateResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // 앱 레벨 중복 체크와 저장 사이의 동시 요청을 DB UNIQUE 제약이 잡아낸 경우
            throw new CustomException(DepartureStationErrorCode.DUPLICATE_DEPARTURE_STATION);
        }
    }

    public void deleteDepartureStation(Long memberId, Long departureStationId) {
        MemberDepartureStation departureStation = memberDepartureStationRepository
                .findByIdAndMemberId(departureStationId, memberId)
                .orElseThrow(() -> new CustomException(DepartureStationErrorCode.DEPARTURE_STATION_NOT_FOUND));
        memberDepartureStationRepository.delete(departureStation);
    }
}
