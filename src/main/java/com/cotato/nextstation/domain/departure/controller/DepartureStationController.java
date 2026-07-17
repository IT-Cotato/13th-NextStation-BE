package com.cotato.nextstation.domain.departure.controller;

import com.cotato.nextstation.domain.departure.dto.request.DepartureStationCreateRequest;
import com.cotato.nextstation.domain.departure.dto.response.DepartureStationResponse;
import com.cotato.nextstation.domain.departure.service.command.DepartureStationCommandService;
import com.cotato.nextstation.domain.departure.service.query.DepartureStationQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/departure-stations")
public class DepartureStationController {

    // TODO: Auth 적용 시 X-Member-Id 헤더를 @AuthenticationPrincipal 로 교체한다.
    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    private final DepartureStationCommandService departureStationCommandService;
    private final DepartureStationQueryService departureStationQueryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<DepartureStationResponse> addDepartureStation(
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Valid @RequestBody DepartureStationCreateRequest request) {
        return CommonResponse.success(HttpStatus.CREATED,
                departureStationCommandService.addDepartureStation(memberId, request));
    }

    @GetMapping
    public CommonResponse<List<DepartureStationResponse>> getDepartureStations(
            @RequestHeader(MEMBER_ID_HEADER) Long memberId) {
        return CommonResponse.success(departureStationQueryService.getDepartureStations(memberId));
    }

    @DeleteMapping("/{departureStationId}")
    public CommonResponse<Void> deleteDepartureStation(
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @PathVariable Long departureStationId) {
        departureStationCommandService.deleteDepartureStation(memberId, departureStationId);
        return CommonResponse.<Void>success(null);
    }
}
