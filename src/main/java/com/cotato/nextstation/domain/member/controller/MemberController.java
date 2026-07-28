package com.cotato.nextstation.domain.member.controller;

import com.cotato.nextstation.domain.member.dto.response.MemberProfileResponse;
import com.cotato.nextstation.domain.member.service.query.MemberQueryService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberQueryService memberQueryService;

    @Operation(
            summary = "내 프로필 조회",
            description = """
                    로그인한 회원의 닉네임/프로필 이미지를 조회한다.
                    - accessToken 인증 필요. 우측 상단 자물쇠(Authorize) 버튼을 눌러 로그인 API 응답의 accessToken 값을(Bearer 접두사 없이) 넣으면 된다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원 (`MemberErrorCode.MEMBER_NOT_FOUND`)"),
    })
    @GetMapping("/me")
    public CommonResponse<MemberProfileResponse> getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal) {
        return CommonResponse.success(memberQueryService.getMyProfile(principal.memberId()));
    }
}