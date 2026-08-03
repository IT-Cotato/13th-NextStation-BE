package com.cotato.nextstation.domain.stamp.controller;

import com.cotato.nextstation.domain.stamp.dto.response.MemberStampListResponse;
import com.cotato.nextstation.domain.stamp.service.query.MemberStampQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import com.cotato.nextstation.global.security.AuthenticationPrincipal;
import com.cotato.nextstation.global.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 다른 회원 프로필의 스탬프 탭. MemberController와 URL prefix가 같지만 컨트롤러는 도메인별로 분리한다(MyCourseController와 같은 방식).
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members/{memberId}/stamps")
public class MemberStampController {

    private final MemberStampQueryService memberStampQueryService;

    @Operation(
            summary = "다른 회원 스탬프 목록 조회",
            description = """
                    다른 회원이 모은 스탬프(방문한 역) 목록을 최근 방문순으로 조회한다.
                    - accessToken 인증 필요.
                    - 프로필 화면의 스탬프 탭에서 사용한다.
                    - 같은 역에서 여러 코스를 완료했어도 역당 스탬프 1개로 묶어서 보여준다.
                    - 전체 목록을 한 번에 내려주며 페이지네이션은 없다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원 (`MemberErrorCode.MEMBER_NOT_FOUND`)"),
    })
    @GetMapping
    public CommonResponse<MemberStampListResponse> getMemberStamps(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "조회할 회원 ID", example = "2")
            @PathVariable Long memberId) {
        return CommonResponse.success(memberStampQueryService.getMemberStamps(memberId));
    }
}
