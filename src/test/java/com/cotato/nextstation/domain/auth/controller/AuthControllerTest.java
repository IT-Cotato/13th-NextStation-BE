package com.cotato.nextstation.domain.auth.controller;

import com.cotato.nextstation.domain.auth.dto.request.SignupRequest;
import com.cotato.nextstation.domain.auth.dto.request.SignupVerificationSendRequest;
import com.cotato.nextstation.domain.auth.dto.response.SignupResponse;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.exception.TermsErrorCode;
import com.cotato.nextstation.domain.auth.service.command.EmailVerificationCommandService;
import com.cotato.nextstation.domain.auth.service.command.SignupCommandService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    EmailVerificationCommandService emailVerificationCommandService;

    @MockitoBean
    SignupCommandService signupCommandService;

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
}
