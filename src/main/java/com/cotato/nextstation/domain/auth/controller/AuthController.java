package com.cotato.nextstation.domain.auth.controller;

import com.cotato.nextstation.domain.auth.dto.request.EmailVerificationConfirmRequest;
import com.cotato.nextstation.domain.auth.dto.request.LoginRequest;
import com.cotato.nextstation.domain.auth.dto.request.ProfileSetupRequest;
import com.cotato.nextstation.domain.auth.dto.request.SignupRequest;
import com.cotato.nextstation.domain.auth.dto.request.SignupVerificationSendRequest;
import com.cotato.nextstation.domain.auth.dto.response.LoginResponse;
import com.cotato.nextstation.domain.auth.dto.response.ProfileSetupResponse;
import com.cotato.nextstation.domain.auth.dto.response.SignupResponse;
import com.cotato.nextstation.domain.auth.service.command.EmailVerificationCommandService;
import com.cotato.nextstation.domain.auth.service.command.ProfileSetupCommandService;
import com.cotato.nextstation.domain.auth.service.command.SignupCommandService;
import com.cotato.nextstation.domain.auth.service.query.LoginQueryService;
import com.cotato.nextstation.domain.auth.service.query.LoginResult;
import com.cotato.nextstation.global.common.response.CommonResponse;
import com.cotato.nextstation.global.util.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final EmailVerificationCommandService emailVerificationCommandService;
    private final SignupCommandService signupCommandService;
    private final ProfileSetupCommandService profileSetupCommandService;
    private final LoginQueryService loginQueryService;

    @Operation(
            summary = "회원가입 이메일 인증번호 발송",
            description = """
                    입력한 이메일로 6자리 인증번호를 발송한다.
                    - 인증번호 유효시간: 3분
                    - 발송 한도: 시간당 5회 / 하루 10회, 초과 시 일정 시간 잠금
                    - 이미 PENDING 상태의 코드가 있으면 새 코드 발송 시 기존 코드는 즉시 무효화된다.
                    - 필수 약관에 모두 동의해야 발송된다(`agreedTermsIds`에 현재 필수 약관 id가 전부 포함돼야 함).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 또는 필수 약관 미동의 (`GlobalErrorCode.VALIDATION_ERROR`, `TermsErrorCode.REQUIRED_TERMS_NOT_AGREED`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 약관 id (`TermsErrorCode.TERMS_NOT_FOUND`)"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일 (`AuthErrorCode.DUPLICATE_EMAIL`)"),
            @ApiResponse(responseCode = "429", description = "발송 횟수 한도 초과 또는 잠금 상태 (`AuthErrorCode.EMAIL_VERIFICATION_RATE_LIMIT_EXCEEDED`)"),
            @ApiResponse(responseCode = "502", description = "메일 발송 실패 (`GlobalErrorCode.EXTERNAL_API_ERROR`)"),
    })
    @PostMapping("/email/verification")
    public CommonResponse<Void> sendEmailVerificationCode(@Valid @RequestBody SignupVerificationSendRequest request) {
        emailVerificationCommandService.sendSignupVerificationCode(request.email(), request.agreedTermsIds());
        return CommonResponse.success(null);
    }

    @Operation(
            summary = "회원가입 이메일 인증번호 확인",
            description = """
                    발송된 6자리 인증번호가 맞는지 확인한다.
                    - 인증번호 불일치 시도는 최대 5회까지 허용되며, 초과 시 해당 인증번호는 실패 처리된다.
                    - 만료된 인증번호로 확인을 시도하면 즉시 실패 처리된다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패, 만료된 인증번호, 또는 인증번호 불일치 (`GlobalErrorCode.VALIDATION_ERROR`, `AuthErrorCode.EMAIL_VERIFICATION_EXPIRED`, `AuthErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH`)"),
            @ApiResponse(responseCode = "404", description = "유효한 인증번호 발송 내역 없음 (`AuthErrorCode.EMAIL_VERIFICATION_NOT_FOUND`)"),
            @ApiResponse(responseCode = "429", description = "인증번호 확인 시도 횟수 초과 (`AuthErrorCode.EMAIL_VERIFICATION_ATTEMPT_EXCEEDED`)"),
    })
    @PostMapping("/email/verification/confirm")
    public CommonResponse<Void> confirmEmailVerificationCode(@Valid @RequestBody EmailVerificationConfirmRequest request) {
        emailVerificationCommandService.verifySignupCode(request.email(), request.code());
        return CommonResponse.success(null);
    }

    @Operation(
            summary = "회원가입 비밀번호 설정",
            description = """
                    이메일 인증이 완료된 이메일로 회원(Member)을 생성하고 비밀번호를 설정한다.
                    - Member 생성과 약관 동의(`member_terms_agreement`) 저장이 한 트랜잭션으로 처리된다.
                    - 응답으로 받는 `signupToken`은 **다음 단계인 "프로필 설정" API를 호출할 때만 쓰는 전용 토큰**이다.
                      로그인 상태를 유지하는 access token이 아니므로 다른 API 호출에는 쓸 수 없고, 발급 후 30분이 지나면 만료된다.
                    - 프로필 설정 화면으로 넘어갈 때 이 값을 프론트에서 잠깐 들고 있다가, 프로필 설정 API 요청의 `Authorization: Bearer {signupToken}` 헤더에 그대로 실어서 보내면 된다.
                    - access token(로그인 유지용)과 refresh token은 이 API가 아니라 로그인 API에서 발급된다.
                    - signupToken이 만료된 뒤 돌아온 **PENDING 회원**이 같은 이메일/비밀번호로 다시 요청하면, 새로 가입시키지 않고 **signupToken만 재발급**한다(비밀번호가 재인증 역할). 이미 프로필 설정까지 끝난 회원은 그대로 `DUPLICATE_EMAIL`로 거부된다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공 또는 PENDING 회원 signupToken 재발급 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패, 비밀번호 확인 불일치, 이메일 인증 미완료, 또는 필수 약관 미동의 (`GlobalErrorCode.VALIDATION_ERROR`, `AuthErrorCode.PASSWORD_CONFIRMATION_MISMATCH`, `AuthErrorCode.EMAIL_NOT_VERIFIED`, `TermsErrorCode.REQUIRED_TERMS_NOT_AGREED`)"),
            @ApiResponse(responseCode = "401", description = "PENDING 회원의 비밀번호 불일치 (`AuthErrorCode.PASSWORD_MISMATCH`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 약관 id (`TermsErrorCode.TERMS_NOT_FOUND`)"),
            @ApiResponse(responseCode = "409", description = "이미 프로필 설정까지 완료된 이메일 (`AuthErrorCode.DUPLICATE_EMAIL`)"),
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/signup")
    public CommonResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        SignupResponse response = signupCommandService.signup(
                request.email(),
                request.password(),
                request.passwordConfirm(),
                request.agreedTermsIds(),
                ClientIpResolver.resolve(httpRequest)
        );
        return CommonResponse.success(HttpStatus.CREATED, response);
    }

    @Operation(
            summary = "프로필 설정",
            description = """
                    닉네임/프로필 사진/성별/생년월일을 설정하고 회원가입을 완료한다(status: PENDING -> ACTIVE).
                    - signupToken 인증 필요. 우측 상단 자물쇠(Authorize) 버튼을 눌러 회원가입 비밀번호 설정 API 응답의 signupToken 값을 그대로(Bearer 접두사 없이) 넣으면 된다.
                    - 프로필 사진은 선택 입력이며, presigned URL로 S3에 업로드 완료 후 받은 imageUrl을 그대로 넣으면 된다.
                    - 성별의 "선택 안함"도 `UNSPECIFIED`로 명시적으로 보내야 한다.
                    - 이미 프로필 설정이 완료된 회원(status != PENDING)이 같은 토큰으로 다시 요청하면 거부된다(재사용 방지).
                    """
    )
    @SecurityRequirement(name = "signupTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 설정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패, 닉네임 길이/문자/금칙어 위반 (`GlobalErrorCode.VALIDATION_ERROR`, `NicknameErrorCode.NICKNAME_TOO_SHORT`, `NicknameErrorCode.NICKNAME_TOO_LONG`, `NicknameErrorCode.NICKNAME_INVALID_CHARACTER`, `NicknameErrorCode.NICKNAME_CONTAINS_BANNED_WORD`)"),
            @ApiResponse(responseCode = "401", description = "signupToken 누락, 위변조, 또는 만료 (`AuthErrorCode.INVALID_SIGNUP_TOKEN`, `AuthErrorCode.SIGNUP_TOKEN_EXPIRED`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원 (`AuthErrorCode.MEMBER_NOT_FOUND`)"),
            @ApiResponse(responseCode = "409", description = "이미 프로필 설정 완료됨 또는 닉네임 중복 (`AuthErrorCode.PROFILE_ALREADY_COMPLETED`, `NicknameErrorCode.DUPLICATE_NICKNAME`)"),
    })
    @PostMapping("/profile")
    public CommonResponse<ProfileSetupResponse> setupProfile(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false, defaultValue = "") String authorizationHeader,
            @Valid @RequestBody ProfileSetupRequest request) {
        ProfileSetupResponse response = profileSetupCommandService.setupProfile(
                authorizationHeader,
                request.nickname(),
                request.profileImageUrl(),
                request.gender(),
                request.birthDate()
        );
        return CommonResponse.success(response);
    }

    @Operation(
            summary = "로그인",
            description = """
                    이메일/비밀번호로 로그인하고 access token을 발급한다.
                    - access token은 응답 body로 내려가며, 다른 API 호출 시 `Authorization: Bearer {accessToken}` 헤더에 실어서 보낸다. 발급 후 1시간이 지나면 만료된다.
                    - refresh token은 httpOnly 쿠키(`refreshToken`)로 내려가며, 프론트에서 직접 다루지 않는다. 발급 후 14일이 지나면 만료된다.
                    - 존재하지 않는 이메일, 비밀번호 불일치, 프로필 설정이 끝나지 않은 회원(PENDING) 모두 동일하게 `INVALID_CREDENTIALS`로 응답한다(계정 존재 여부 노출 방지).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 (`GlobalErrorCode.VALIDATION_ERROR`)"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치 (`AuthErrorCode.INVALID_CREDENTIALS`)"),
    })
    @PostMapping("/login")
    public CommonResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse httpResponse) {
        LoginResult result = loginQueryService.login(request.email(), request.password());

        ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(LoginQueryService.REFRESH_TOKEN_EXPIRATION)
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return CommonResponse.success(new LoginResponse(result.memberId(), result.accessToken()));
    }
}