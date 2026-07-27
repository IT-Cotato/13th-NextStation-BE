package com.cotato.nextstation.domain.stamp.controller;

import com.cotato.nextstation.domain.stamp.dto.response.CourseCompleteResponse;
import com.cotato.nextstation.domain.stamp.dto.response.StationPopularCoursesResponse;
import com.cotato.nextstation.domain.stamp.service.command.StampCommandService;
import com.cotato.nextstation.domain.stamp.service.query.StampCourseQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class StampCourseController {

    // TODO: Auth 적용 시 X-Member-Id 헤더를 @AuthenticationPrincipal 로 교체한다.
    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String MEMBER_ID_DESCRIPTION = "회원 ID (Auth 적용 전까지 사용하는 임시 헤더)";

    private final StampCommandService stampCommandService;
    private final StampCourseQueryService stampCourseQueryService;


    @Operation(
            summary = "코스 완료 처리",
            description = """
                    코스를 완료 처리하고 스탬프를 획득한다.
                    - 본인이 만든 코스만 완료할 수 있다.
                    - 같은 코스를 여러 번 완료할 수 있다 (재방문 허용).
                    - 완료 시 발급된 memberStampId는 여행일지 작성 시 사용한다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "완료 성공"),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님 (`StampErrorCode.COURSE_NOT_OWNED`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 코스 (`CourseErrorCode.COURSE_NOT_FOUND`)"),
    })
    @PostMapping("/courses/{courseId}/complete")
    public CommonResponse<CourseCompleteResponse> completeCourse(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Parameter(description = "코스 ID", example = "1")
            @PathVariable Long courseId) {
        return CommonResponse.success(stampCommandService.completeCourse(memberId, courseId));
    }


    @Operation(
            summary = "역별 인기코스 조회",
            description = """
                    스탬프 페이지 내에서 특정 역의 인기 코스 상위 3개를 조회한다.
                    - 인기순: 조회수 + 저장수×2 내림차순
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요함"),
    })
    @GetMapping("/stamps/stations/{stationId}/courses")
    public CommonResponse<StationPopularCoursesResponse> getPopularCoursesByStation(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Parameter(description = "역 ID", example = "12")
            @PathVariable Long stationId) {
        return CommonResponse.success(stampCourseQueryService.getPopularCoursesByStation(stationId));
    }

}