package com.cotato.nextstation.domain.member.controller;

import com.cotato.nextstation.domain.member.dto.request.MemberProfileUpdateRequest;
import com.cotato.nextstation.domain.member.dto.response.MemberProfileResponse;
import com.cotato.nextstation.domain.member.service.command.MemberCommandService;
import com.cotato.nextstation.domain.member.service.query.MemberQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import com.cotato.nextstation.global.security.AuthenticationPrincipal;
import com.cotato.nextstation.global.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;

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

    @Operation(
            summary = "프로필 수정",
            description = """
                    로그인한 회원의 닉네임/프로필 이미지를 부분 수정한다.
                    - 요청 body에 넘어온 필드만 수정되고, 생략한 필드는 기존 값을 유지한다.
                    - `nickname`을 보내면 한글/영문/숫자 2~10자, 중복·금칙어 검증을 거쳐 변경된다. 현재 닉네임과 같은 값이면 검증 없이 그대로 유지된다.
                    - `profileImageUrl`을 보내면 presigned URL로 S3 업로드 완료 후 받은 imageUrl로 교체된다. 빈 문자열(`""`)을 보내면 프로필 이미지를 제거한다.
                    - 이미지가 교체되거나 제거되면 기존에 쓰던 S3 이미지 파일은 자동으로 삭제된다.
                    - accessToken 인증 필요. 우측 상단 자물쇠(Authorize) 버튼을 눌러 로그인 API 응답의 accessToken 값을(Bearer 접두사 없이) 넣으면 된다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패, 닉네임 길이/문자/금칙어 위반, 또는 허용되지 않은 프로필 이미지 URL (`GlobalErrorCode.VALIDATION_ERROR`, `NicknameErrorCode.NICKNAME_TOO_SHORT`, `NicknameErrorCode.NICKNAME_TOO_LONG`, `NicknameErrorCode.NICKNAME_INVALID_CHARACTER`, `NicknameErrorCode.NICKNAME_CONTAINS_BANNED_WORD`, `MemberErrorCode.INVALID_PROFILE_IMAGE_URL`)"),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원 (`MemberErrorCode.MEMBER_NOT_FOUND`)"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임 (`NicknameErrorCode.DUPLICATE_NICKNAME`)"),
    })
    @PatchMapping("/me")
    public CommonResponse<MemberProfileResponse> updateMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody MemberProfileUpdateRequest request) {
        MemberProfileResponse response = memberCommandService.updateMyProfile(
                principal.memberId(), request.nickname(), request.profileImageUrl());
        return CommonResponse.success(response);
    }
}