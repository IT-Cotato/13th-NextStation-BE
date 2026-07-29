package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.MyCourseDeleteRequest;
import com.cotato.nextstation.domain.course.dto.response.MyCourseListResponse;
import com.cotato.nextstation.domain.course.service.command.CourseLikeCommandService;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import com.cotato.nextstation.global.security.AuthenticationPrincipal;
import com.cotato.nextstation.global.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// CourseController와 같은 태그를 써서 Swagger에서 한 섹션으로 보이게 한다.
@Tag(name = "Course")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members/me/courses")
public class MyCourseController {

    private final CourseQueryService courseQueryService;
    private final CourseLikeCommandService courseLikeCommandService;

    @Operation(
            summary = "내가 만든 코스 목록 조회",
            description = """
                    저장 탭에서 내가 만든 코스를 최신순으로 조회한다.
                    - 본인 코스이므로 공개 여부와 무관하게 전부 보여준다.
                    - `lineId`/`stationId`는 선택 사항이며, 함께 주면 둘 다 만족하는 코스만 나온다.
                    - `lineId` 필터는 역이 **속한 호선 전체**를 기준으로 한다. 환승역 코스는 소속된 모든 호선에서
                      조회되므로, 한 코스가 여러 호선 탭에 노출될 수 있다.
                      (카드의 `line`은 배지 표시용 대표 호선 하나이며 필터 기준과 별개다)
                    - `availableLines`는 코스가 없는 호선 칩을 비활성화하는 데 쓴다.
                      필터와 같은 기준(소속 호선 전체)이라 환승역 때문에 1~9호선 외 노선이 포함될 수 있다.
                      현재 필터와 무관하게 전체 기준이며, 최초 조회(cursor 없음)에서만 채워진다.
                    - `isCompleted`는 여행 완료 처리(`POST /courses/{courseId}/complete`) 여부다.
                      완료한 코스는 스탬프가 채워진 모양으로 표시한다.
                    - `nextCursor`를 그대로 `cursor`에 넣어 다음 페이지를 요청한다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "size 범위를 벗어남 (`GlobalErrorCode.INVALID_PAGE_SIZE`) 또는 커서가 잘못됨 (`GlobalErrorCode.INVALID_CURSOR`)"),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
    })
    @GetMapping
    public CommonResponse<MyCourseListResponse> getMyCourses(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "호선 필터 (생략하면 전체)", example = "6")
            @RequestParam(required = false) Long lineId,
            @Parameter(description = "역 필터 (생략하면 전체)", example = "6")
            @RequestParam(required = false) Long stationId,
            @Parameter(description = "다음 페이지 커서 (첫 페이지는 생략)")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기 (1~50, 기본 10)", example = "10")
            @RequestParam(required = false) Integer size) {
        return CommonResponse.success(courseQueryService.getMyCourses(principal.memberId(), lineId, stationId, cursor, size));
    }

    @Operation(
            summary = "내가 만든 코스 다중 삭제",
            description = """
                    저장 탭 선택 모드에서 여러 코스를 골라 한 번에 삭제한다.
                    - 본인 코스만 대상이 되며, 선택한 코스 중 본인 것이 아니거나 이미 삭제된 것이
                      섞여 있어도 나머지는 정상 삭제된다(부분 성공 허용).
                    - 하나도 삭제 대상이 없을 때만 404로 응답한다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공 (data 없음)"),
            @ApiResponse(responseCode = "400", description = "삭제할 코스를 선택하지 않음 (`GlobalErrorCode.VALIDATION_ERROR`)"),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "404", description = "선택한 코스가 모두 없거나 본인 코스가 아님 (`CourseErrorCode.COURSE_NOT_FOUND`)"),
    })
    @DeleteMapping
    public CommonResponse<Void> deleteMyCourses(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody MyCourseDeleteRequest request) {
        courseLikeCommandService.deleteCourses(principal.memberId(), request.courseIds());
        return CommonResponse.success(null);
    }
}
