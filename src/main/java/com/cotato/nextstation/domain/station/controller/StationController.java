package com.cotato.nextstation.domain.station.controller;

import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.service.query.StationQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stations")
public class StationController {

    private final StationQueryService stationQueryService;

    @Operation(
            summary = "역 검색",
            description = """
                    역명으로 역을 검색한다. 
                    - 검색 대상은 서울 내 전체 역 
                    - 현재는 이름 전체일치 검색이다.
                    - 환승역이면 소속 노선이 여러 개로 반환된다.
                    - 일치하는 역이 없으면 빈 목록을 반환한다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공 (결과 없으면 빈 목록)"),
    })
    @GetMapping
    public CommonResponse<List<StationSummaryResponse>> searchStations(
            @Parameter(description = "검색할 역명 (전체일치)", example = "왕십리역")
            @RequestParam String keyword) {
        return CommonResponse.success(stationQueryService.searchByName(keyword));
    }
}
