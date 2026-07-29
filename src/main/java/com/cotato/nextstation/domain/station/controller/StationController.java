package com.cotato.nextstation.domain.station.controller;

import com.cotato.nextstation.domain.station.dto.response.StationPlacesResponse;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.service.query.StationQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
                    - **부분일치** 검색이다. 예를 들어 "십리"로 검색하면 왕십리역·답십리역·상왕십리역이 모두 나온다.
                    - 역명 끝의 "역"은 떼고 비교하므로 "왕십리"와 "왕십리역" 중 무엇을 입력해도 결과가 같다.
                    - 같은 이유로 **"역" 한 글자로 검색하면 이름 안쪽에 "역"이 든 역만** 나온다.
                      (역삼역·역촌역·동대문역사문화공원역·암사역사공원역) 모든 역명이 "역"으로 끝나므로
                      꼬리의 "역"은 검색 대상이 아니다.
                    - 정렬은 **완전일치 → 접두사일치 → 나머지 포함** 순이고, 같은 순위 안에서는 역명 오름차순이다.
                      ("서울역" 검색 시 서울역이 서울대입구역보다 위에 온다)
                    - 결과는 **최대 20개**까지만 반환한다.
                    - 환승역이면 소속 노선이 여러 개로 반환된다.
                    - 검색어가 비어 있거나 일치하는 역이 없으면 빈 목록을 반환한다(404가 아니다).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공 (결과 없으면 빈 목록)"),
    })
    @GetMapping
    public CommonResponse<List<StationSummaryResponse>> searchStations(
            @Parameter(description = "검색할 역명 (부분일치)", example = "십리")
            @RequestParam String keyword) {
        return CommonResponse.success(stationQueryService.searchByName(keyword));
    }

    @Operation(
            summary = "역별 장소 목록 조회",
            description = """
                    코스 만들기 화면에서 고를 수 있는 장소 후보를 카테고리별로 묶어 반환한다.
                    - 카테고리 노출 순서: 문화공간 → 식당 → 카페 → 산책포인트
                    - 카테고리당 최대 3개다. 후보가 더 많으면 잘라내고, 적으면 있는 만큼 내려준다.
                    - 호출할 때마다 같은 결과가 나오도록 순서를 고정한다.
                    - 장소가 없는 카테고리는 응답에서 빠진다.
                    - 뽑기 대상이 아닌 역은 장소가 없어 빈 목록으로 응답한다.
                    - `defaultCourseName`은 코스 저장 시 기본으로 채울 이름이며 사용자가 수정할 수 있다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (장소가 없으면 빈 목록)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 역 (`StationErrorCode.STATION_NOT_FOUND`)"),
    })
    @GetMapping("/{stationId}/places")
    public CommonResponse<StationPlacesResponse> getStationPlaces(
            @Parameter(description = "역 ID", example = "6")
            @PathVariable Long stationId) {
        return CommonResponse.success(stationQueryService.getStationPlaces(stationId));
    }
}
