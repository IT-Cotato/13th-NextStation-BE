package com.cotato.nextstation.domain.member.controller;

import com.cotato.nextstation.domain.member.dto.response.MemberProfileResponse;
import com.cotato.nextstation.domain.member.dto.response.OtherMemberProfileResponse;
import com.cotato.nextstation.domain.member.exception.MemberErrorCode;
import com.cotato.nextstation.domain.member.service.query.MemberQueryService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
import com.cotato.nextstation.global.jwt.JwtProvider;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MemberControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MemberQueryService memberQueryService;

    // WebConfig가 등록하는 JwtPrincipalArgumentResolver가 필요로 해서 @WebMvcTest 슬라이스에도 목이 필요하다
    @MockitoBean
    JwtProvider jwtProvider;

    private static final String TOKEN = "access-token";

    @Test
    @DisplayName("정상 accessToken이면 닉네임/프로필 이미지를 반환한다")
    void getMyProfile_success() throws Exception {
        // given
        given(jwtProvider.parseClaims(TOKEN)).willReturn(
                Jwts.claims().subject("1").add("purpose", "ACCESS").build());
        given(memberQueryService.getMyProfile(1L))
                .willReturn(new MemberProfileResponse(1L, "환승러", "https://cdn.example.com/profile/1.png"));

        // when & then
        mockMvc.perform(get("/api/v1/members/me")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(1L))
                .andExpect(jsonPath("$.data.nickname").value("환승러"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://cdn.example.com/profile/1.png"));
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 401을 반환한다")
    void getMyProfile_missingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_401_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 404를 반환한다")
    void getMyProfile_memberNotFound() throws Exception {
        // given
        given(jwtProvider.parseClaims(TOKEN)).willReturn(
                Jwts.claims().subject("1").add("purpose", "ACCESS").build());
        given(memberQueryService.getMyProfile(1L))
                .willThrow(new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/members/me")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("다른 회원 프로필 조회 성공 시 닉네임/프로필 이미지/스탬프 개수/공개 코스 개수를 반환한다")
    void getMemberProfile_success() throws Exception {
        // given
        given(jwtProvider.parseClaims(TOKEN)).willReturn(
                Jwts.claims().subject("1").add("purpose", "ACCESS").build());
        given(memberQueryService.getMemberProfile(2L))
                .willReturn(new OtherMemberProfileResponse(
                        2L, "환승러2", "https://cdn.example.com/profile/2.png", 12L, 5L));

        // when & then
        mockMvc.perform(get("/api/v1/members/2/profile")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(2L))
                .andExpect(jsonPath("$.data.nickname").value("환승러2"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://cdn.example.com/profile/2.png"))
                .andExpect(jsonPath("$.data.stampCount").value(12))
                .andExpect(jsonPath("$.data.publicCourseCount").value(5));
    }

    @Test
    @DisplayName("다른 회원 프로필 조회 시 Authorization 헤더가 없으면 401을 반환한다")
    void getMemberProfile_missingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/members/2/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_401_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("존재하지 않는 회원의 프로필을 조회하면 404를 반환한다")
    void getMemberProfile_memberNotFound() throws Exception {
        // given
        given(jwtProvider.parseClaims(TOKEN)).willReturn(
                Jwts.claims().subject("1").add("purpose", "ACCESS").build());
        given(memberQueryService.getMemberProfile(2L))
                .willThrow(new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/members/2/profile")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCode()));
    }
}