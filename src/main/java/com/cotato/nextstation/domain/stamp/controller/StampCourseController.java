package com.cotato.nextstation.domain.stamp.controller;

import com.cotato.nextstation.domain.stamp.dto.response.StationPopularCoursesResponse;
import com.cotato.nextstation.domain.stamp.service.query.StampCourseQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stamps/stations")
public class StampCourseController {

    private final StampCourseQueryService stampCourseQueryService;

    @Operation(
            summary = "역별 인기코스 조회",
            description = """
                    스탬프 페이지 내에서 특정 역의 인기 코스 상위 3개를 조회한다.
                    - 인기순: 조회수 + 저장수×2 내림차순
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
    })
    @GetMapping("/{stationId}/courses")
    public CommonResponse<StationPopularCoursesResponse> getPopularCoursesByStation(
            @Parameter(description = "역 ID", example = "12")
            @PathVariable Long stationId) {
        return CommonResponse.success(stampCourseQueryService.getPopularCoursesByStation(stationId));
    }
}