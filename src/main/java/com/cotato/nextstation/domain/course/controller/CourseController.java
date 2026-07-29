package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.CourseCopyRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseCreateRequest;
import com.cotato.nextstation.domain.course.dto.request.ExploreCourseCondition;
import com.cotato.nextstation.domain.course.dto.request.CourseUpdateRequest;
import com.cotato.nextstation.domain.course.dto.response.CourseCreateResponse;
import com.cotato.nextstation.domain.course.dto.response.ExploreCourseListResponse;
import com.cotato.nextstation.domain.course.entity.CourseSort;
import com.cotato.nextstation.domain.course.dto.response.CourseUpdateResponse;
import com.cotato.nextstation.domain.course.service.command.CourseCommandService;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 저장 탭 API는 경로가 /members/me 하위라 컨트롤러가 나뉘는데,
// 같은 태그를 달아 Swagger에서는 한 섹션으로 묶는다.
@Tag(name = "Course")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseCommandService courseCommandService;
    private final CourseLikeCommandService courseLikeCommandService;
    private final CourseQueryService courseQueryService;

    @Operation(
            summary = "둘러보기 코스 목록 조회",
            description = """
                    둘러보기의 노선따라 둘러보기와 코스 검색이 이 API를 함께 쓴다.
                    - 공개된 여행일지가 있는 코스만 나온다.
                    - 카드를 누르면 `journalId`로 여행일지 상세를 연다. 목록에 그 값이 함께 내려간다.
                    - `lineId`/`stationId`/`keyword`는 모두 선택 사항이며, 함께 주면 전부 만족하는 코스만 나온다.
                    - `lineId` 필터는 역이 **속한 호선 전체**를 기준으로 한다. 환승역 코스는 소속된 모든 호선에서 조회된다.
                    - `keyword`는 **코스 이름과 역명**만 검색한다. 역명은 꼬리의 `역`을 떼고 비교하므로
                      `신림`과 `신림역` 중 무엇을 넣어도 결과가 같다.
                    - `sort`는 `LATEST`(기본, 최신순) 또는 `POPULAR`(조회수 + 좋아요×2, 동률이면 최신순)다.
                      화면의 "전체"는 `LATEST`로 보내면 된다.
                    - **정렬을 바꾸면 커서를 버리고 첫 페이지부터 다시 요청해야 한다.** 정렬마다 커서 구조가 달라
                      이전 커서를 그대로 보내면 400이다.
                    - `isLiked`는 로그인했을 때만 채워지며, 비로그인이면 항상 false다.
                    - `imageUrl`은 작성자가 여행일지에 올린 첫 사진이다. 아직 사진 데이터가 없어 현재는 null이다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (결과 없으면 빈 목록)"),
            @ApiResponse(responseCode = "400", description = "size 범위를 벗어남 (`GlobalErrorCode.INVALID_PAGE_SIZE`) 또는 커서가 잘못됨/정렬과 맞지 않음 (`GlobalErrorCode.INVALID_CURSOR`)"),
            @ApiResponse(responseCode = "401", description = "accessToken을 보냈으나 위변조 또는 만료 (`GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
    })
    @GetMapping
    public CommonResponse<ExploreCourseListResponse> getExploreCourses(
            // 비로그인도 둘러볼 수 있어야 해서 required = false다. 로그인했을 때만 하트 상태를 채운다.
            @Parameter(hidden = true) @AuthenticationPrincipal(required = false) JwtPrincipal principal,
            @Parameter(description = "호선 필터 (생략하면 전체)", example = "2")
            @RequestParam(required = false) Long lineId,
            @Parameter(description = "역 필터 (생략하면 전체)", example = "123")
            @RequestParam(required = false) Long stationId,
            @Parameter(description = "검색어 (코스 이름·역명)", example = "신림")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "정렬 기준 (기본 LATEST)", example = "LATEST")
            @RequestParam(required = false) CourseSort sort,
            @Parameter(description = "다음 페이지 커서 (첫 페이지는 생략)")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기 (1~50, 기본 10)", example = "10")
            @RequestParam(required = false) Integer size) {
        Long memberId = (principal != null) ? principal.memberId() : null;
        ExploreCourseCondition condition = new ExploreCourseCondition(lineId, stationId, keyword, null);
        return CommonResponse.success(
                courseQueryService.getExploreCourses(memberId, condition, sort, cursor, size));
    }

    @Operation(
            summary = "사람들이 많이 찾는 코스 조회",
            description = """
                    둘러보기의 "사람들이 많이 찾는 코스"다. 좋아요 수가 많은 순으로, 동률이면 최신순이다.
                    - **상위 30개까지만** 보여준다. 30번째를 넘어가면 `hasNext`가 false다.
                    - 둘러보기 목록(`GET /api/v1/courses`)의 `sort=POPULAR`(조회수 + 좋아요×2)와는
                      **다른 기준**이다. 여기는 담은 횟수(좋아요 수)만 본다.
                    - 순위 번호는 내려주지 않는다. 정렬된 순서 그대로 프론트에서 매기면 된다.
                    - `cursor`에는 다음 시작 위치가 담긴다. 그대로 다음 요청에 넣으면 된다.
                    - 공개된 여행일지가 있는 코스만 나오며, 카드를 누르면 `journalId`로 여행일지 상세를 연다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (결과 없으면 빈 목록)"),
            @ApiResponse(responseCode = "400", description = "size 범위를 벗어남 (`GlobalErrorCode.INVALID_PAGE_SIZE`) 또는 커서가 잘못됨 (`GlobalErrorCode.INVALID_CURSOR`)"),
            @ApiResponse(responseCode = "401", description = "accessToken을 보냈으나 위변조 또는 만료 (`GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
    })
    @GetMapping("/popular")
    public CommonResponse<ExploreCourseListResponse> getMostLikedCourses(
            // 비로그인도 둘러볼 수 있어야 해서 required = false다.
            @Parameter(hidden = true) @AuthenticationPrincipal(required = false) JwtPrincipal principal,
            @Parameter(description = "다음 페이지 커서 (첫 페이지는 생략)")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기 (1~50, 기본 10)", example = "10")
            @RequestParam(required = false) Integer size) {
        Long memberId = (principal != null) ? principal.memberId() : null;
        return CommonResponse.success(courseQueryService.getMostLikedCourses(memberId, cursor, size));
    }

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
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = """
                    요청 값 검증 실패 (`GlobalErrorCode.VALIDATION_ERROR`)
                    또는 같은 장소 중복 선택 (`CourseErrorCode.DUPLICATE_COURSE_PLACES`)"""),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<CourseCreateResponse> createCourse(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CourseCreateRequest request) {
        return CommonResponse.success(HttpStatus.CREATED, courseCommandService.createCourse(principal.memberId(), request));
    }

    @Operation(
            summary = "내 코스로 만들기",
            description = """
                    다른 사람의 공개 코스를 편집 가능한 내 코스로 복제한다.
                    - `{courseId}`는 **복사할 원본 코스**의 ID다.
                    - 원본을 참조만 하는 좋아요와 달리, 복제본은 원본이 삭제되거나 비공개로 바뀌어도 그대로 남는다.
                    - `name`은 **복제본에 부여할 이름**이며 필수다. 사용자가 이름을 고치지 않았으면
                      화면에 채워둔 **원본 이름을 그대로 실어 보낸다**. 
                    - `placeIds`를 넘기면 그 순서대로 코스 순서가 부여되고, 생략하면 원본 순서를 그대로 따른다.
                    - 장소를 빼거나 더할 수는 없어 `placeIds`는 원본의 장소 구성과 정확히 일치해야 한다.
                    - 복제본의 조회수·좋아요 수는 0부터 시작하며, 여행일지·컨셉투어는 물려받지 않는다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "복제 성공"),
            @ApiResponse(responseCode = "400", description = """
                    요청 값 검증 실패 (`GlobalErrorCode.VALIDATION_ERROR`),
                    본인이 만든 코스를 복사 (`CourseErrorCode.CANNOT_COPY_OWN_COURSE`),
                    장소 목록이 원본 구성과 불일치 (`CourseErrorCode.INVALID_COURSE_PLACES`)
                    또는 같은 장소 중복 (`CourseErrorCode.DUPLICATE_COURSE_PLACES`)"""),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 공개되지 않은 코스 (`CourseErrorCode.COURSE_NOT_FOUND`)"),
    })
    @PostMapping("/{courseId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<CourseCreateResponse> copyCourse(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "복사할 원본 코스 ID", example = "1")
            @PathVariable Long courseId,
            @Valid @RequestBody CourseCopyRequest request) {
        return CommonResponse.success(HttpStatus.CREATED,
                courseCommandService.copyCourse(principal.memberId(), courseId, request));
    }

    @Operation(
            summary = "코스 수정",
            description = """
                    본인이 만든 코스의 이름·장소 순서를 수정한다. name/placeIds는 각각 선택 사항이며,
                    요청에 있는 필드만 반영한다(둘 다 생략하면 400).
                    - name: 최대 20자, 공백 불가
                    - placeIds: 코스의 기존 장소 구성과 정확히 일치해야 한다(개수·구성 모두). 배열 순서대로 order_num이 재할당된다.
                    - 한 트랜잭션으로 처리되어, 장소 순서 검증에 실패하면 이름 변경도 함께 롤백된다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = """
                    이름·장소 순서 모두 생략, 이름이 공백이거나 20자 초과, 장소가 3개 미만/10개 초과
                    (`GlobalErrorCode.VALIDATION_ERROR`), 장소 목록이 기존 코스 구성과 불일치
                    (`CourseErrorCode.INVALID_COURSE_PLACES`) 또는 같은 장소 중복 (`CourseErrorCode.DUPLICATE_COURSE_PLACES`)"""),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님 (`CourseErrorCode.COURSE_FORBIDDEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 코스 (`CourseErrorCode.COURSE_NOT_FOUND`)"),
    })
    @PatchMapping("/{courseId}")
    public CommonResponse<CourseUpdateResponse> updateCourse(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "코스 ID", example = "1")
            @PathVariable Long courseId,
            @Valid @RequestBody CourseUpdateRequest request) {
        return CommonResponse.success(courseCommandService.updateCourse(principal.memberId(), courseId, request));
    }

    @Operation(
            summary = "코스 좋아요 추가",
            description = """
                    다른 사람의 코스를 보관함에 좋아요한다.
                    - 좋아요는 원본을 참조만 하므로, 원본이 삭제되거나 비공개로 바뀌면 목록에서도 빠진다.
                    - 코스를 복사해 내 것으로 만드는 "내 코스로 만들기"와는 다른 기능이다.
                    - 좋아요하면 해당 코스의 좋아요 수가 1 증가한다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "좋아요 성공 (data 없음)"),
            @ApiResponse(responseCode = "400", description = "본인이 만든 코스에 좋아요 (`CourseErrorCode.CANNOT_LIKE_OWN_COURSE`)"),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 코스 (`CourseErrorCode.COURSE_NOT_FOUND`)"),
            @ApiResponse(responseCode = "409", description = "이미 좋아요한 코스 (`CourseErrorCode.DUPLICATE_COURSE_LIKE`)"),
    })
    @PostMapping("/{courseId}/likes")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<Void> likeCourse(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "코스 ID", example = "1")
            @PathVariable Long courseId) {
        courseLikeCommandService.likeCourse(principal.memberId(), courseId);
        return CommonResponse.success(HttpStatus.CREATED, null);
    }

    @Operation(
            summary = "코스 좋아요 단건 취소",
            description = """
                    보관함에서 좋아요한 코스를 제거한다.
                    - 원본 코스는 삭제되지 않는다.
                    - 취소하면 해당 코스의 좋아요 수가 1 감소한다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공 (data 없음)"),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "404", description = "좋아요하지 않은 코스 (`CourseErrorCode.COURSE_LIKE_NOT_FOUND`)"),
    })
    @DeleteMapping("/{courseId}/likes")
    public CommonResponse<Void> cancelCourseLike(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "코스 ID", example = "1")
            @PathVariable Long courseId) {
        courseLikeCommandService.cancelLike(principal.memberId(), courseId);
        return CommonResponse.success(null);
    }

    @Operation(
            summary = "내가 만든 코스 단건 삭제",
            description = """
                    내가 만든 코스를 삭제한다.
                    - soft delete이며, 삭제 후에는 목록·상세 조회에서 모두 제외된다.
                    - 이 코스를 좋아요한 사람들의 보관함에서도 함께 사라진다.
                    - 저장 탭 선택 모드에서 여러 코스를 한 번에 지울 때는
                      `DELETE /api/v1/members/me/courses`(다중 삭제)를 사용한다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공 (data 없음)"),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님 (`CourseErrorCode.COURSE_DELETE_FORBIDDEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 코스 (`CourseErrorCode.COURSE_NOT_FOUND`)"),
    })
    @DeleteMapping("/{courseId}")
    public CommonResponse<Void> deleteCourse(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "코스 ID", example = "1")
            @PathVariable Long courseId) {
        courseLikeCommandService.deleteCourse(principal.memberId(), courseId);
        return CommonResponse.success(null);
    }
}
