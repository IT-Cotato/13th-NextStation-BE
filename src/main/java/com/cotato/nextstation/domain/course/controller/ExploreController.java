package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.response.ExploreResponse;
import com.cotato.nextstation.domain.course.service.query.ExploreQueryService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 둘러보기 메인은 코스 섹션을 조합한 화면이라 Course 도메인이 소유한다.
@Tag(name = "Course")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/explore")
public class ExploreController {

    private final ExploreQueryService exploreQueryService;

    @Operation(
            summary = "둘러보기 메인 조회",
            description = """
                    둘러보기 탭 첫 화면이다. 세 섹션을 한 번에 내려준다.
                    - `popularCourses`: 사람들이 많이 찾는 코스 **6개** (좋아요 수 순)
                    - `conceptTours`: 컨셉별 투어 **3개** (표시 순서대로)
                    - `lines` + `selectedLineId` + `lineCourses`: 노선 칩과 처음 선택된 노선의 코스 **3개**

                    더보기나 칩 전환부터는 개별 API를 쓴다.
                    - 많이 찾는 코스 더보기 → `GET /api/v1/courses/popular`
                    - 컨셉별 투어 더보기 → `GET /api/v1/concept-tours`
                    - 노선 칩 전환 → `GET /api/v1/courses?lineId=`

                    `lines`에는 **공개 코스가 하나라도 있는 노선만** 담긴다. 칩을 눌렀는데 빈 목록이
                    나오지 않도록 한 것이며, `selectedLineId`는 그 목록의 첫 번째다.

                    로그인 없이 조회할 수 있고, 로그인했을 때만 카드의 `isLiked`가 채워진다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (공개 코스가 없으면 각 목록이 빈 배열)"),
            @ApiResponse(responseCode = "401", description = "accessToken을 보냈으나 위변조 또는 만료 (`GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
    })
    @GetMapping
    public CommonResponse<ExploreResponse> getExplore(
            // 비로그인도 둘러볼 수 있어야 해서 required = false다.
            @Parameter(hidden = true) @AuthenticationPrincipal(required = false) JwtPrincipal principal) {
        Long memberId = (principal != null) ? principal.memberId() : null;
        return CommonResponse.success(exploreQueryService.getExplore(memberId));
    }
}
