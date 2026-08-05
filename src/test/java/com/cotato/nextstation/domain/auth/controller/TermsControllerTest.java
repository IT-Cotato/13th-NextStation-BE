package com.cotato.nextstation.domain.auth.controller;

import com.cotato.nextstation.domain.auth.dto.response.TermsResponse;
import com.cotato.nextstation.domain.auth.dto.response.TermsSummaryResponse;
import com.cotato.nextstation.domain.auth.entity.TermsType;
import com.cotato.nextstation.domain.auth.service.query.TermsQueryService;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
import com.cotato.nextstation.global.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TermsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TermsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TermsQueryService termsQueryService;

    // WebConfig가 등록하는 JwtPrincipalArgumentResolver가 필요로 해서 @WebMvcTest 슬라이스에도 목이 필요하다
    @MockitoBean
    JwtProvider jwtProvider;

    @Test
    @DisplayName("약관 목록은 필수 약관 먼저 정렬된 상태로, 원문 없이 반환된다")
    void getTerms_success() throws Exception {
        given(termsQueryService.getLatestTerms()).willReturn(List.of(
                new TermsSummaryResponse(1L, TermsType.SERVICE, "서비스 이용약관", "v1.0", true),
                new TermsSummaryResponse(2L, TermsType.MARKETING, "마케팅 정보 수신 동의", "v1.0", false)
        ));

        mockMvc.perform(get("/api/v1/auth/terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].type").value("SERVICE"))
                .andExpect(jsonPath("$.data[0].isRequired").value(true))
                .andExpect(jsonPath("$.data[0].content").doesNotExist())
                .andExpect(jsonPath("$.data[1].isRequired").value(false));
    }

    @Test
    @DisplayName("약관 단건 조회는 type으로 원문까지 반환한다")
    void getTermsByType_success() throws Exception {
        given(termsQueryService.getLatestTerms(TermsType.SERVICE))
                .willReturn(new TermsResponse(1L, TermsType.SERVICE, "서비스 이용약관", "제1조 (목적)", "v1.0", true));

        mockMvc.perform(get("/api/v1/auth/terms/SERVICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("SERVICE"))
                .andExpect(jsonPath("$.data.content").value("제1조 (목적)"));
    }

    @Test
    @DisplayName("정의되지 않은 약관 종류면 400을 반환한다")
    void getTermsByType_invalidType() throws Exception {
        mockMvc.perform(get("/api/v1/auth/terms/OPENSOURCE"))
                .andExpect(status().isBadRequest());
    }
}
