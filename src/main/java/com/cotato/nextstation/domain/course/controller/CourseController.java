package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.CourseCreateRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseNameUpdateRequest;
import com.cotato.nextstation.domain.course.dto.request.CoursePlaceOrderUpdateRequest;
import com.cotato.nextstation.domain.course.dto.response.CourseCreateResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseNameResponse;
import com.cotato.nextstation.domain.course.service.CourseCommandService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class CourseController {

    // TODO: Auth 적용 시 X-Member-Id 헤더를 @AuthenticationPrincipal 로 교체한다.
    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String MEMBER_ID_DESCRIPTION = "회원 ID (Auth 적용 전까지 사용하는 임시 헤더)";

    private final CourseCommandService courseCommandService;

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
}
