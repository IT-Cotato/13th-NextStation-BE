package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.CourseSaveCancelAllRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseSaveCancelRequest;
import com.cotato.nextstation.domain.course.dto.response.SavedCourseListResponse;
import com.cotato.nextstation.domain.course.service.command.CourseSaveCommandService;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// CourseController와 같은 태그를 써서 Swagger에서 한 섹션으로 보이게 한다.
@Tag(name = "Course")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members/me/saved-courses")
public class SavedCourseController {

    // TODO: Auth 적용 시 X-Member-Id 헤더를 @AuthenticationPrincipal 로 교체한다.
    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String MEMBER_ID_DESCRIPTION = "회원 ID (Auth 적용 전까지 사용하는 임시 헤더)";

    private final CourseSaveCommandService courseSaveCommandService;
    private final CourseQueryService courseQueryService;

    @Operation(
            summary = "스크랩한 코스 목록 조회",
            description = """
                    저장 탭에서 내가 스크랩한 코스를 최근 스크랩순으로 조회한다.
                    - 원본 코스가 삭제되거나 비공개로 바뀌면 목록에서 빠진다(스크랩은 원본 참조).
                    - 화면에 필터 칩이 없어 호선/역 필터를 받지 않는다.
                    - `nextCursor`를 그대로 `cursor`에 넣어 다음 페이지를 요청한다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "size 범위를 벗어남 (`GlobalErrorCode.INVALID_PAGE_SIZE`) 또는 커서가 잘못됨 (`GlobalErrorCode.INVALID_CURSOR`)"),
    })
    @GetMapping
    public CommonResponse<SavedCourseListResponse> getSavedCourses(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Parameter(description = "다음 페이지 커서 (첫 페이지는 생략)")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기 (1~50, 기본 10)", example = "10")
            @RequestParam(required = false) Integer size) {
        return CommonResponse.success(courseQueryService.getSavedCourses(memberId, cursor, size));
    }

    @Operation(
            summary = "코스 스크랩 전체 취소",
            description = """
                    저장 탭에서 "모두 선택"으로 스크랩을 한 번에 취소한다.
                    - 갤러리의 전체 선택처럼 **아직 화면에 불러오지 않은 스크랩까지** 취소된다.
                      그래서 코스 ID를 받지 않고 서버가 대상을 다시 조회한다.
                    - 전체 선택 후 일부를 해제했다면 그 코스 ID를 `exceptCourseIds`로 보낸다.
                    - 목록에 보이는 스크랩만 대상이다(원본이 비공개로 바뀐 스크랩은 취소되지 않는다).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공 (data 없음)"),
            @ApiResponse(responseCode = "404", description = "취소할 스크랩이 없음 (`CourseErrorCode.COURSE_SAVE_NOT_FOUND`)"),
    })
    @DeleteMapping("/all")
    public CommonResponse<Void> cancelAllCourseSaves(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @RequestBody(required = false) CourseSaveCancelAllRequest request) {
        courseSaveCommandService.cancelAllSaves(memberId, request == null ? null : request.exceptCourseIds());
        return CommonResponse.success(null);
    }

    @Operation(
            summary = "코스 스크랩 다중 취소",
            description = """
                    저장 탭에서 여러 코스를 선택해 한 번에 스크랩을 취소한다.
                    - 코스 상세에서 북마크를 하나만 해제할 때는 `DELETE /courses/{courseId}/saves`를 사용한다.
                    - 요청한 코스 중 이미 취소된 것이 섞여 있어도 나머지는 정상 취소된다(부분 성공 허용).
                    - 하나도 저장돼 있지 않을 때만 404로 응답한다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공 (data 없음)"),
            @ApiResponse(responseCode = "400", description = "취소할 코스를 선택하지 않음 (`GlobalErrorCode.VALIDATION_ERROR`)"),
            @ApiResponse(responseCode = "404", description = "선택한 코스가 모두 저장돼 있지 않음 (`CourseErrorCode.COURSE_SAVE_NOT_FOUND`)"),
    })
    @DeleteMapping
    public CommonResponse<Void> cancelCourseSaves(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Valid @RequestBody CourseSaveCancelRequest request) {
        courseSaveCommandService.cancelSaves(memberId, request.courseIds());
        return CommonResponse.success(null);
    }
}
