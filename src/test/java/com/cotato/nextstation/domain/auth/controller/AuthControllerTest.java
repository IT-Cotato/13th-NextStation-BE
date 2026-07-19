package com.cotato.nextstation.domain.auth.controller;

import com.cotato.nextstation.domain.auth.dto.request.SignupVerificationSendRequest;
import com.cotato.nextstation.domain.auth.exception.TermsErrorCode;
import com.cotato.nextstation.domain.auth.service.command.EmailVerificationCommandService;
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
}
