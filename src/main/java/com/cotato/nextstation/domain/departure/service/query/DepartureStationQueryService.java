package com.cotato.nextstation.domain.departure.service.query;

import com.cotato.nextstation.domain.departure.converter.DepartureStationConverter;
import com.cotato.nextstation.domain.departure.dto.response.DepartureStationResponse;
import com.cotato.nextstation.domain.departure.repository.MemberDepartureStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartureStationQueryService {

    private final MemberDepartureStationRepository memberDepartureStationRepository;
    private final DepartureStationConverter departureStationConverter;

    public List<DepartureStationResponse> getDepartureStations(Long memberId) {
        return departureStationConverter.toResponses(
                memberDepartureStationRepository.findByMemberIdOrderByOrderNumAsc(memberId));
    }
}
