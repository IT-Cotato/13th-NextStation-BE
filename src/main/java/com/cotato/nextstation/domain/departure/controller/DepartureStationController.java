package com.cotato.nextstation.domain.departure.controller;

import com.cotato.nextstation.domain.departure.dto.request.DepartureStationCreateRequest;
import com.cotato.nextstation.domain.departure.dto.response.DepartureStationCreateResponse;
import com.cotato.nextstation.domain.departure.dto.response.DepartureStationResponse;
import com.cotato.nextstation.domain.departure.service.command.DepartureStationCommandService;
import com.cotato.nextstation.domain.departure.service.query.DepartureStationQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    private static final String MEMBER_ID_DESCRIPTION = "회원 ID (Auth 적용 전까지 사용하는 임시 헤더)";

    private final DepartureStationCommandService departureStationCommandService;
    private final DepartureStationQueryService departureStationQueryService;

    @Operation(
            summary = "출발역 즐겨찾기 추가",
            description = """
                    자주 쓰는 출발역을 즐겨찾기에 추가한다.
                    - 회원당 최대 10개까지 저장 가능하다.
                    - 추가 응답에는 역명/노선이 포함되지 않는다. (역 정보는 목록 조회에서 제공)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "추가 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 (`GlobalErrorCode.VALIDATION_ERROR`)"),
            @ApiResponse(responseCode = "409", description = """
                    최대 10개 초과 (`DepartureStationErrorCode.MAX_DEPARTURE_STATIONS_EXCEEDED`)
                    또는 이미 추가한 역 (`DepartureStationErrorCode.DUPLICATE_DEPARTURE_STATION`)"""),
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<DepartureStationCreateResponse> addDepartureStation(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Valid @RequestBody DepartureStationCreateRequest request) {
        return CommonResponse.success(HttpStatus.CREATED,
                departureStationCommandService.addDepartureStation(memberId, request));
    }

    @Operation(
            summary = "출발역 즐겨찾기 목록 조회",
            description = """
                    회원이 저장한 출발역 즐겨찾기를 표시 순서대로 조회한다.
                    - 각 항목에 역명/노선이 포함된다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (없으면 빈 목록)"),
    })
    @GetMapping
    public CommonResponse<List<DepartureStationResponse>> getDepartureStations(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId) {
        return CommonResponse.success(departureStationQueryService.getDepartureStations(memberId));
    }

    @Operation(
            summary = "출발역 즐겨찾기 삭제",
            description = "본인이 저장한 출발역 즐겨찾기를 삭제한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공 (data 없음)"),
            @ApiResponse(responseCode = "404", description = "본인 소유가 아니거나 존재하지 않음 "),
    })
    @DeleteMapping("/{departureStationId}")
    public CommonResponse<Void> deleteDepartureStation(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Parameter(description = "출발역 즐겨찾기 ID", example = "5")
            @PathVariable Long departureStationId) {
        departureStationCommandService.deleteDepartureStation(memberId, departureStationId);
        return CommonResponse.<Void>success(null);
    }
}
