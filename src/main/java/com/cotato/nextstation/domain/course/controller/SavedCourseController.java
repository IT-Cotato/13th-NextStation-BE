package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.CourseSaveCancelRequest;
import com.cotato.nextstation.domain.course.service.command.CourseSaveCommandService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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
