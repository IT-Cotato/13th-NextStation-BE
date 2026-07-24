package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.CourseCopyRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseCreateRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseNameUpdateRequest;
import com.cotato.nextstation.domain.course.dto.request.CoursePlaceOrderUpdateRequest;
import com.cotato.nextstation.domain.course.dto.response.CourseCreateResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseNameResponse;
import com.cotato.nextstation.domain.course.service.command.CourseCommandService;
import com.cotato.nextstation.domain.course.service.command.CourseSaveCommandService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 저장 탭 API는 경로가 /members/me 하위라 컨트롤러가 나뉘는데,
// 같은 태그를 달아 Swagger에서는 한 섹션으로 묶는다.
@Tag(name = "Course")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class CourseController {

    // TODO: Auth 적용 시 X-Member-Id 헤더를 @AuthenticationPrincipal 로 교체한다.
    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String MEMBER_ID_DESCRIPTION = "회원 ID (Auth 적용 전까지 사용하는 임시 헤더)";

    private final CourseCommandService courseCommandService;
    private final CourseSaveCommandService courseSaveCommandService;

    @Operation(
            summary = "코스 생성",
            description = """
                    선택한 장소들로 코스를 생성한다.
                    - 장소는 카테고리 무관 3개 이상 10개 이하, 같은 장소 중복 선택 불가
                    - 코스 이름은 최대 20자
                    - placeIds 순서대로 order_num이 부여된다
                    - journalId는 여행일지 작성 시, conceptTourId는 관리자 큐레이션으로 추후 채워진다
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = """
                    요청 값 검증 실패 (`GlobalErrorCode.VALIDATION_ERROR`)
                    또는 같은 장소 중복 선택 (`CourseErrorCode.DUPLICATE_COURSE_PLACES`)"""),
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<CourseCreateResponse> createCourse(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Valid @RequestBody CourseCreateRequest request) {
        return CommonResponse.success(HttpStatus.CREATED, courseCommandService.createCourse(memberId, request));
    }

    @Operation(
            summary = "내 코스로 만들기",
            description = """
                    다른 사람의 공개 코스를 편집 가능한 내 코스로 복제한다.
                    - `{courseId}`는 **복사할 원본 코스**의 ID다.
                    - 원본을 참조만 하는 스크랩과 달리, 복제본은 원본이 삭제되거나 비공개로 바뀌어도 그대로 남는다.
                    - `placeIds`를 넘기면 그 순서대로 코스 순서가 부여되고, 생략하면 원본 순서를 그대로 따른다.
                    - 장소를 빼거나 더할 수는 없어 `placeIds`는 원본의 장소 구성과 정확히 일치해야 한다.
                    - 복제본의 조회수·저장 수는 0부터 시작하며, 여행일지·컨셉투어는 물려받지 않는다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "복제 성공"),
            @ApiResponse(responseCode = "400", description = """
                    요청 값 검증 실패 (`GlobalErrorCode.VALIDATION_ERROR`),
                    본인이 만든 코스를 복사 (`CourseErrorCode.CANNOT_COPY_OWN_COURSE`),
                    장소 목록이 원본 구성과 불일치 (`CourseErrorCode.INVALID_COURSE_PLACES`)
                    또는 같은 장소 중복 (`CourseErrorCode.DUPLICATE_COURSE_PLACES`)"""),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 공개되지 않은 코스 (`CourseErrorCode.COURSE_NOT_FOUND`)"),
    })
    @PostMapping("/{courseId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<CourseCreateResponse> copyCourse(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Parameter(description = "복사할 원본 코스 ID", example = "1")
            @PathVariable Long courseId,
            @Valid @RequestBody CourseCopyRequest request) {
        return CommonResponse.success(HttpStatus.CREATED,
                courseCommandService.copyCourse(memberId, courseId, request));
    }

    @Operation(
            summary = "코스 이름 수정",
            description = "본인이 만든 코스의 이름을 수정한다. (최대 20자)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 (`GlobalErrorCode.VALIDATION_ERROR`)"),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님 (`CourseErrorCode.COURSE_FORBIDDEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 코스 (`CourseErrorCode.COURSE_NOT_FOUND`)"),
    })
    @PatchMapping("/{courseId}/name")
    public CommonResponse<CourseNameResponse> updateCourseName(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Parameter(description = "코스 ID", example = "1")
            @PathVariable Long courseId,
            @Valid @RequestBody CourseNameUpdateRequest request) {
        return CommonResponse.success(courseCommandService.updateCourseName(memberId, courseId, request));
    }

    @Operation(
            summary = "코스 내 장소 순서 수정",
            description = """
                    요청한 placeIds 배열 순서대로 코스 장소의 order_num을 재할당한다.
                    - placeIds는 해당 코스의 장소 구성과 정확히 일치해야 한다 (개수·구성 모두)
                    - 반환 데이터는 없다 (`CommonResponse<Void>`)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공 (data 없음)"),
            @ApiResponse(responseCode = "400", description = """
                    장소 목록이 기존 코스 구성과 불일치 (`CourseErrorCode.INVALID_COURSE_PLACES`)
                    또는 같은 장소 중복 (`CourseErrorCode.DUPLICATE_COURSE_PLACES`)"""),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님 (`CourseErrorCode.COURSE_FORBIDDEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 코스 (`CourseErrorCode.COURSE_NOT_FOUND`)"),
    })
    @PatchMapping("/{courseId}/places/order")
    public CommonResponse<Void> updateCoursePlaceOrder(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Parameter(description = "코스 ID", example = "1")
            @PathVariable Long courseId,
            @Valid @RequestBody CoursePlaceOrderUpdateRequest request) {
        courseCommandService.updateCoursePlaceOrder(memberId, courseId, request);
        return CommonResponse.success(null);
    }

    @Operation(
            summary = "코스 스크랩 추가",
            description = """
                    다른 사람의 코스를 보관함에 스크랩한다.
                    - 스크랩은 원본을 참조만 하므로, 원본이 삭제되거나 비공개로 바뀌면 목록에서도 빠진다.
                    - 코스를 복사해 내 것으로 만드는 "내 코스로 만들기"와는 다른 기능이다.
                    - 스크랩하면 해당 코스의 저장 수가 1 증가한다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "스크랩 성공 (data 없음)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 코스 (`CourseErrorCode.COURSE_NOT_FOUND`)"),
            @ApiResponse(responseCode = "409", description = "이미 저장한 코스 (`CourseErrorCode.DUPLICATE_COURSE_SAVE`)"),
    })
    @PostMapping("/{courseId}/saves")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<Void> saveCourse(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Parameter(description = "코스 ID", example = "1")
            @PathVariable Long courseId) {
        courseSaveCommandService.saveCourse(memberId, courseId);
        return CommonResponse.success(HttpStatus.CREATED, null);
    }

    @Operation(
            summary = "코스 스크랩 취소",
            description = """
                    보관함에서 스크랩한 코스를 제거한다.
                    - 원본 코스는 삭제되지 않는다.
                    - 취소하면 해당 코스의 저장 수가 1 감소한다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공 (data 없음)"),
            @ApiResponse(responseCode = "404", description = "저장하지 않은 코스 (`CourseErrorCode.COURSE_SAVE_NOT_FOUND`)"),
    })
    @DeleteMapping("/{courseId}/saves")
    public CommonResponse<Void> cancelCourseSave(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Parameter(description = "코스 ID", example = "1")
            @PathVariable Long courseId) {
        courseSaveCommandService.cancelSave(memberId, courseId);
        return CommonResponse.success(null);
    }

    @Operation(
            summary = "코스 삭제",
            description = """
                    내가 만든 코스를 삭제한다.
                    - soft delete이며, 삭제 후에는 목록·상세 조회에서 모두 제외된다.
                    - 이 코스를 스크랩한 사람들의 보관함에서도 함께 사라진다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공 (data 없음)"),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님 (`CourseErrorCode.COURSE_DELETE_FORBIDDEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 코스 (`CourseErrorCode.COURSE_NOT_FOUND`)"),
    })
    @DeleteMapping("/{courseId}")
    public CommonResponse<Void> deleteCourse(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Parameter(description = "코스 ID", example = "1")
            @PathVariable Long courseId) {
        courseSaveCommandService.deleteCourse(memberId, courseId);
        return CommonResponse.success(null);
    }
}
