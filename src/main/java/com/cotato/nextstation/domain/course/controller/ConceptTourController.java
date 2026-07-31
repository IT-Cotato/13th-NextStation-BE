package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.response.ConceptTourResponse;
import com.cotato.nextstation.domain.course.dto.response.ExploreCourseListResponse;
import com.cotato.nextstation.domain.course.entity.CourseSort;
import com.cotato.nextstation.domain.course.service.query.ConceptTourQueryService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 컨셉투어는 코스를 묶는 분류라 Course 도메인이 소유하되, 화면은 둘러보기 안에만 있어 경로를 그 아래 둔다.
@Tag(name = "Explore")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/explore/concept-tours")
public class ConceptTourController {

    private final ConceptTourQueryService conceptTourQueryService;
    private final CourseQueryService courseQueryService;

    @Operation(
            summary = "컨셉별 투어 목록 조회",
            description = """
                    둘러보기의 "컨셉별 투어"에 쓴다. 관리자가 정한 표시 순서대로 전부 내려준다.
                    - 컨셉이 여덟 개 남짓이라 **페이징하지 않는다.**
                    - 화면의 검색창은 이 목록을 받아 프론트에서 걸러내면 된다. 서버는 검색어를 받지 않는다.
                    - `courseCount`는 목록에 실제로 보이는 것과 같도록 **공개된 코스만** 센다.
                    - 둘러보기 탭은 로그인 필수라 토큰이 없으면 401이다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (컨셉이 없으면 빈 목록)"),
            @ApiResponse(responseCode = "401", description = "accessToken이 없거나 위변조·만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
    })
    @GetMapping
    public CommonResponse<List<ConceptTourResponse>> getConceptTours(
            // 개인화 데이터는 없지만, 둘러보기 탭 전체가 로그인 필수라 다른 조회와 계약을 맞춘다.
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal) {
        return CommonResponse.success(conceptTourQueryService.getConceptTours());
    }

    @Operation(
            summary = "컨셉별 코스 목록 조회",
            description = """
                    컨셉 상세 화면의 코스 목록이다. 정렬 토글(전체/최신순/인기순)에 대응한다.
                    - `sort`는 `LATEST`(기본, 최신순) 또는 `POPULAR`(조회수 + 좋아요×2)다.
                      화면의 "전체"는 `LATEST`로 보내면 된다.
                    - 카드 구조와 커서 사용법은 둘러보기 목록(`GET /api/v1/explore/courses`)과 같다.
                    - 이 화면에는 정렬 토글만 있고 노선·역 필터가 없어 `availableStations`는 항상 빈 배열이다.
                    - **정렬을 바꾸면 커서를 버리고 첫 페이지부터 다시 요청해야 한다.**
                    - 없는 컨셉이거나 속한 코스가 없으면 빈 목록이다.
                    - 둘러보기 탭은 로그인 필수다. `isLiked`는 요청한 회원 기준으로 항상 채워진다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (결과 없으면 빈 목록)"),
            @ApiResponse(responseCode = "400", description = "size 범위를 벗어남 (`GlobalErrorCode.INVALID_PAGE_SIZE`) 또는 커서가 잘못됨/정렬과 맞지 않음 (`GlobalErrorCode.INVALID_CURSOR`)"),
            @ApiResponse(responseCode = "401", description = "accessToken이 없거나 위변조·만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
    })
    @GetMapping("/{conceptTourId}/courses")
    public CommonResponse<ExploreCourseListResponse> getConceptTourCourses(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "컨셉투어 ID", example = "1")
            @PathVariable Long conceptTourId,
            @Parameter(description = "정렬 기준 (기본 LATEST)", example = "LATEST")
            @RequestParam(required = false) CourseSort sort,
            @Parameter(description = "다음 페이지 커서 (첫 페이지는 생략)")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기 (1~50, 기본 10)", example = "10")
            @RequestParam(required = false) Integer size) {
        Long memberId = principal.memberId();
        return CommonResponse.success(
                courseQueryService.getConceptTourCourses(memberId, conceptTourId, sort, cursor, size));
    }
}
