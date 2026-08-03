package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.response.CourseCardResponse;
import com.cotato.nextstation.domain.course.dto.response.MemberCourseListResponse;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.domain.member.exception.MemberErrorCode;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.LineCode;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import com.cotato.nextstation.global.jwt.JwtProvider;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberCourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MemberCourseControllerTest {

    private static final String TOKEN = "access-token";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CourseQueryService courseQueryService;

    // WebConfig가 등록하는 JwtPrincipalArgumentResolver가 필요로 해서 @WebMvcTest 슬라이스에도 목이 필요하다
    @MockitoBean
    JwtProvider jwtProvider;

    @BeforeEach
    void authenticateAsMember1() {
        given(jwtProvider.parseClaims(TOKEN)).willReturn(
                Jwts.claims().subject("1").add("purpose", "ACCESS").build());
    }

    @Test
    @DisplayName("다른 회원 공개 코스 목록은 200과 코스 카드/다음 커서를 반환한다")
    void getMemberCourses_success() throws Exception {
        // given
        given(courseQueryService.getMemberPublicCourses(2L, null, null)).willReturn(
                new MemberCourseListResponse(
                        List.of(new CourseCardResponse(7L, "보문역 환승여행 코스", 6L, "보문역",
                                new LineSummaryResponse(6L, "6호선", LineCode.LINE_6))),
                        "eyJpZCI6MjB9", true));

        // when & then
        mockMvc.perform(get("/api/v1/members/2/courses").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courses[0].courseId").value(7L))
                .andExpect(jsonPath("$.data.courses[0].stationName").value("보문역"))
                .andExpect(jsonPath("$.data.nextCursor").value("eyJpZCI6MjB9"))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 401을 반환한다")
    void getMemberCourses_missingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/members/2/courses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_401_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 404를 반환한다")
    void getMemberCourses_memberNotFound() throws Exception {
        // given
        given(courseQueryService.getMemberPublicCourses(2L, null, null))
                .willThrow(new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/members/2/courses").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(MemberErrorCode.MEMBER_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("size 범위를 벗어나면 400을 반환한다")
    void getMemberCourses_invalidPageSize() throws Exception {
        // given
        willThrow(new CustomException(GlobalErrorCode.INVALID_PAGE_SIZE))
                .given(courseQueryService).getMemberPublicCourses(2L, null, 100);

        // when & then
        mockMvc.perform(get("/api/v1/members/2/courses")
                        .header("Authorization", "Bearer " + TOKEN)
                        .param("size", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_PAGE_SIZE.getCode()));
    }
}
