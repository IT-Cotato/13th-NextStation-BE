package com.cotato.nextstation.domain.auth.controller;

import com.cotato.nextstation.domain.auth.dto.request.LoginRequest;
import com.cotato.nextstation.domain.auth.dto.request.ProfileSetupRequest;
import com.cotato.nextstation.domain.auth.dto.request.SignupRequest;
import com.cotato.nextstation.domain.auth.dto.request.SignupVerificationSendRequest;
import com.cotato.nextstation.domain.auth.dto.response.ProfileSetupResponse;
import com.cotato.nextstation.domain.auth.dto.response.SignupResponse;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.exception.TermsErrorCode;
import com.cotato.nextstation.domain.auth.service.command.EmailVerificationCommandService;
import com.cotato.nextstation.domain.auth.service.command.ProfileSetupCommandService;
import com.cotato.nextstation.domain.auth.service.command.SignupCommandService;
import com.cotato.nextstation.domain.auth.service.query.LoginQueryService;
import com.cotato.nextstation.domain.auth.service.query.LoginResult;
import com.cotato.nextstation.domain.auth.service.query.ReissueResult;
import com.cotato.nextstation.domain.member.entity.Gender;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import com.cotato.nextstation.domain.member.exception.NicknameErrorCode;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
import com.cotato.nextstation.global.jwt.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    EmailVerificationCommandService emailVerificationCommandService;

    @MockitoBean
    SignupCommandService signupCommandService;

    @MockitoBean
    ProfileSetupCommandService profileSetupCommandService;

    @MockitoBean
    LoginQueryService loginQueryService;

    // WebConfig가 등록하는 JwtPrincipalArgumentResolver가 필요로 해서 @WebMvcTest 슬라이스에도 목이 필요하다
    @MockitoBean
    JwtProvider jwtProvider;

    @Test
    @DisplayName("동의한 약관 목록이 비어있으면 400을 반환한다")
    void sendEmailVerificationCode_agreedTermsIdsEmpty() throws Exception {
        SignupVerificationSendRequest request = new SignupVerificationSendRequest("user@example.com", List.of());

        mockMvc.perform(post("/api/v1/auth/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.agreedTermsIds").exists());
    }

    @Test
    @DisplayName("필수 약관을 동의하지 않으면 400을 반환한다")
    void sendEmailVerificationCode_requiredTermsNotAgreed() throws Exception {
        SignupVerificationSendRequest request = new SignupVerificationSendRequest("user@example.com", List.of(2L));
        willThrow(new CustomException(TermsErrorCode.REQUIRED_TERMS_NOT_AGREED))
                .given(emailVerificationCommandService).sendSignupVerificationCode("user@example.com", List.of(2L));

        mockMvc.perform(post("/api/v1/auth/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TermsErrorCode.REQUIRED_TERMS_NOT_AGREED.getCode()));
    }

    @Test
    @DisplayName("필수 약관에 모두 동의하면 200을 반환한다")
    void sendEmailVerificationCode_success() throws Exception {
        SignupVerificationSendRequest request = new SignupVerificationSendRequest("user@example.com", List.of(1L, 2L));
        willDoNothing().given(emailVerificationCommandService).sendSignupVerificationCode("user@example.com", List.of(1L, 2L));

        mockMvc.perform(post("/api/v1/auth/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("비밀번호와 비밀번호 확인이 다르면 400을 반환한다")
    void signup_passwordConfirmationMismatch() throws Exception {
        SignupRequest request = new SignupRequest("user@example.com", "abc12345!", "different1!", List.of(1L));
        willThrow(new CustomException(AuthErrorCode.PASSWORD_CONFIRMATION_MISMATCH))
                .given(signupCommandService).signup(anyString(), anyString(), anyString(), any(), anyString());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.PASSWORD_CONFIRMATION_MISMATCH.getCode()));
    }

    @Test
    @DisplayName("이메일 인증이 완료되지 않았으면 400을 반환한다")
    void signup_emailNotVerified() throws Exception {
        SignupRequest request = new SignupRequest("user@example.com", "abc12345!", "abc12345!", List.of(1L));
        willThrow(new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED))
                .given(signupCommandService).signup(anyString(), anyString(), anyString(), any(), anyString());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.EMAIL_NOT_VERIFIED.getCode()));
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 409를 반환한다")
    void signup_duplicateEmail() throws Exception {
        SignupRequest request = new SignupRequest("user@example.com", "abc12345!", "abc12345!", List.of(1L));
        willThrow(new CustomException(AuthErrorCode.DUPLICATE_EMAIL))
                .given(signupCommandService).signup(anyString(), anyString(), anyString(), any(), anyString());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.DUPLICATE_EMAIL.getCode()));
    }

    @Test
    @DisplayName("PENDING 회원의 비밀번호가 다르면 401을 반환한다")
    void signup_passwordMismatchForPendingMember() throws Exception {
        SignupRequest request = new SignupRequest("user@example.com", "abc12345!", "abc12345!", List.of(1L));
        willThrow(new CustomException(AuthErrorCode.PASSWORD_MISMATCH))
                .given(signupCommandService).signup(anyString(), anyString(), anyString(), any(), anyString());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.PASSWORD_MISMATCH.getCode()));
    }

    @Test
    @DisplayName("비밀번호 형식이 올바르지 않으면 400을 반환한다")
    void signup_invalidPasswordFormat() throws Exception {
        SignupRequest request = new SignupRequest("user@example.com", "short1!", "short1!", List.of(1L));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.password").exists());
    }

    @Test
    @DisplayName("정상 요청이면 201과 signupToken을 반환한다")
    void signup_success() throws Exception {
        SignupRequest request = new SignupRequest("user@example.com", "abc12345!", "abc12345!", List.of(1L));
        given(signupCommandService.signup(anyString(), anyString(), anyString(), any(), anyString()))
                .willReturn(new SignupResponse(1L, "signup-token"));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(1L))
                .andExpect(jsonPath("$.data.signupToken").value("signup-token"));
    }

    private static final String SIGNUP_TOKEN_HEADER = "Bearer signup-token";

    private ProfileSetupRequest profileSetupRequest(String nickname) {
        return new ProfileSetupRequest(nickname, null, Gender.MALE, LocalDate.of(2001, 1, 1));
    }

    @Test
    @DisplayName("정상 요청이면 200과 프로필 설정 결과를 반환한다")
    void setupProfile_success() throws Exception {
        ProfileSetupRequest request = profileSetupRequest("환승러");
        ProfileSetupResponse response = new ProfileSetupResponse(1L, "환승러", MemberStatus.ACTIVE);
        given(profileSetupCommandService.setupProfile(SIGNUP_TOKEN_HEADER, "환승러", null, Gender.MALE, LocalDate.of(2001, 1, 1)))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/auth/profile")
                        .header("Authorization", SIGNUP_TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(1L))
                .andExpect(jsonPath("$.data.nickname").value("환승러"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("닉네임이 비어있으면 400을 반환한다")
    void setupProfile_nicknameBlank() throws Exception {
        ProfileSetupRequest request = profileSetupRequest("");

        mockMvc.perform(post("/api/v1/auth/profile")
                        .header("Authorization", SIGNUP_TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.nickname").exists());
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 401을 반환한다")
    void setupProfile_missingAuthorizationHeader() throws Exception {
        ProfileSetupRequest request = profileSetupRequest("환승러");
        willThrow(new CustomException(AuthErrorCode.INVALID_SIGNUP_TOKEN))
                .given(profileSetupCommandService).setupProfile(anyString(), anyString(), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_SIGNUP_TOKEN.getCode()));
    }

    @Test
    @DisplayName("토큰이 만료됐으면 401을 반환한다")
    void setupProfile_expiredToken() throws Exception {
        ProfileSetupRequest request = profileSetupRequest("환승러");
        willThrow(new CustomException(AuthErrorCode.SIGNUP_TOKEN_EXPIRED))
                .given(profileSetupCommandService).setupProfile(anyString(), anyString(), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/profile")
                        .header("Authorization", SIGNUP_TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.SIGNUP_TOKEN_EXPIRED.getCode()));
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 404를 반환한다")
    void setupProfile_memberNotFound() throws Exception {
        ProfileSetupRequest request = profileSetupRequest("환승러");
        willThrow(new CustomException(AuthErrorCode.MEMBER_NOT_FOUND))
                .given(profileSetupCommandService).setupProfile(anyString(), anyString(), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/profile")
                        .header("Authorization", SIGNUP_TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.MEMBER_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("이미 프로필 설정이 완료된 회원이면 409를 반환한다")
    void setupProfile_alreadyCompleted() throws Exception {
        ProfileSetupRequest request = profileSetupRequest("환승러");
        willThrow(new CustomException(AuthErrorCode.PROFILE_ALREADY_COMPLETED))
                .given(profileSetupCommandService).setupProfile(anyString(), anyString(), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/profile")
                        .header("Authorization", SIGNUP_TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.PROFILE_ALREADY_COMPLETED.getCode()));
    }

    @Test
    @DisplayName("닉네임이 중복되면 409를 반환한다")
    void setupProfile_duplicateNickname() throws Exception {
        ProfileSetupRequest request = profileSetupRequest("환승러");
        willThrow(new CustomException(NicknameErrorCode.DUPLICATE_NICKNAME))
                .given(profileSetupCommandService).setupProfile(anyString(), anyString(), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/profile")
                        .header("Authorization", SIGNUP_TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(NicknameErrorCode.DUPLICATE_NICKNAME.getCode()));
    }

    @Test
    @DisplayName("닉네임이 2자 미만이면 400을 반환한다")
    void setupProfile_nicknameTooShort() throws Exception {
        ProfileSetupRequest request = profileSetupRequest("환");
        willThrow(new CustomException(NicknameErrorCode.NICKNAME_TOO_SHORT))
                .given(profileSetupCommandService).setupProfile(anyString(), anyString(), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/profile")
                        .header("Authorization", SIGNUP_TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(NicknameErrorCode.NICKNAME_TOO_SHORT.getCode()));
    }

    @Test
    @DisplayName("닉네임이 10자를 초과하면 400을 반환한다")
    void setupProfile_nicknameTooLong() throws Exception {
        ProfileSetupRequest request = profileSetupRequest("환승러환승러환승러환승러");
        willThrow(new CustomException(NicknameErrorCode.NICKNAME_TOO_LONG))
                .given(profileSetupCommandService).setupProfile(anyString(), anyString(), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/profile")
                        .header("Authorization", SIGNUP_TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(NicknameErrorCode.NICKNAME_TOO_LONG.getCode()));
    }

    @Test
    @DisplayName("닉네임에 허용되지 않은 문자가 포함되면 400을 반환한다")
    void setupProfile_nicknameInvalidCharacter() throws Exception {
        ProfileSetupRequest request = profileSetupRequest("환승러!!");
        willThrow(new CustomException(NicknameErrorCode.NICKNAME_INVALID_CHARACTER))
                .given(profileSetupCommandService).setupProfile(anyString(), anyString(), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/profile")
                        .header("Authorization", SIGNUP_TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(NicknameErrorCode.NICKNAME_INVALID_CHARACTER.getCode()));
    }

    @Test
    @DisplayName("정상 로그인이면 200과 accessToken을 반환하고 refreshToken을 쿠키로 내려준다")
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "abc12345!");
        given(loginQueryService.login("user@example.com", "abc12345!"))
                .willReturn(new LoginResult(1L, "access-token", "refresh-token"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(1L))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(cookie().value("refreshToken", "refresh-token"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    @DisplayName("이메일 또는 비밀번호가 일치하지 않으면 401을 반환한다")
    void login_invalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "wrongpassword1!");
        willThrow(new CustomException(AuthErrorCode.INVALID_CREDENTIALS))
                .given(loginQueryService).login("user@example.com", "wrongpassword1!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_CREDENTIALS.getCode()));
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 400을 반환한다")
    void login_invalidEmailFormat() throws Exception {
        LoginRequest request = new LoginRequest("not-an-email", "abc12345!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.email").exists());
    }

    @Test
    @DisplayName("유효한 refreshToken 쿠키가 있으면 200과 새 accessToken을 반환한다")
    void reissue_success() throws Exception {
        given(loginQueryService.reissue("refresh-token"))
                .willReturn(new ReissueResult(1L, "new-access-token"));

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .cookie(new Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("refreshToken 쿠키가 없으면 401을 반환한다")
    void reissue_missingCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_REFRESH_TOKEN.getCode()));
    }

    @Test
    @DisplayName("refreshToken이 만료됐으면 401을 반환한다")
    void reissue_expiredToken() throws Exception {
        willThrow(new CustomException(AuthErrorCode.REFRESH_TOKEN_EXPIRED))
                .given(loginQueryService).reissue("refresh-token");

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .cookie(new Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.REFRESH_TOKEN_EXPIRED.getCode()));
    }

    @Test
    @DisplayName("refreshToken이 위변조됐거나 purpose가 다르면 401을 반환한다")
    void reissue_invalidToken() throws Exception {
        willThrow(new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN))
                .given(loginQueryService).reissue("bad-token");

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .cookie(new Cookie("refreshToken", "bad-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_REFRESH_TOKEN.getCode()));
    }
}