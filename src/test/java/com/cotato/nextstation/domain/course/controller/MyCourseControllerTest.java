package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.response.CourseCardResponse;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.LineCode;
import com.cotato.nextstation.domain.course.dto.response.MyCourseListResponse;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MyCourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MyCourseControllerTest {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CourseQueryService courseQueryService;

    // WebConfig가 등록하는 JwtPrincipalArgumentResolver가 필요로 해서 @WebMvcTest 슬라이스에도 목이 필요하다
    @MockitoBean
    JwtProvider jwtProvider;

    @Test
    @DisplayName("내 코스 목록은 200과 선택 가능한 호선/코스 카드를 반환한다")
    void getMyCourses_success() throws Exception {
        given(courseQueryService.getMyCourses(1L, null, null, null, null)).willReturn(
                new MyCourseListResponse(
                        List.of(new LineSummaryResponse(1L, "1호선", LineCode.LINE_1),
                                new LineSummaryResponse(6L, "6호선", LineCode.LINE_6)),
                        List.of(new CourseCardResponse(7L, "보문역 환승여행 코스", 6L, "보문역",
                                new LineSummaryResponse(6L, "6호선", LineCode.LINE_6))),
                        null, false));

        mockMvc.perform(get("/api/v1/members/me/courses").header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableLines[0].id").value(1))
                .andExpect(jsonPath("$.data.availableLines[0].name").value("1호선"))
                .andExpect(jsonPath("$.data.availableLines[1].name").value("6호선"))
                .andExpect(jsonPath("$.data.availableLines[1].code").value("LINE_6"))
                .andExpect(jsonPath("$.data.courses[0].courseId").value(7))
                .andExpect(jsonPath("$.data.courses[0].stationName").value("보문역"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("호선/역 필터를 그대로 서비스에 전달한다")
    void getMyCourses_passesFilters() throws Exception {
        given(courseQueryService.getMyCourses(1L, 6L, 9L, null, null))
                .willReturn(new MyCourseListResponse(List.of(), List.of(), null, false));

        mockMvc.perform(get("/api/v1/members/me/courses")
                        .header(MEMBER_ID_HEADER, 1L)
                        .param("lineId", "6")
                        .param("stationId", "9"))
                .andExpect(status().isOk());

        verify(courseQueryService).getMyCourses(1L, 6L, 9L, null, null);
    }

    @Test
    @DisplayName("만든 코스가 없으면 200과 빈 목록을 반환한다")
    void getMyCourses_empty() throws Exception {
        given(courseQueryService.getMyCourses(1L, null, null, null, null))
                .willReturn(new MyCourseListResponse(List.of(), List.of(), null, false));

        mockMvc.perform(get("/api/v1/members/me/courses").header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableLines").isEmpty())
                .andExpect(jsonPath("$.data.courses").isEmpty());
    }

    @Test
    @DisplayName("커서가 잘못되면 400을 반환한다")
    void getMyCourses_invalidCursor() throws Exception {
        given(courseQueryService.getMyCourses(1L, null, null, "broken", null))
                .willThrow(new CustomException(GlobalErrorCode.INVALID_CURSOR));

        mockMvc.perform(get("/api/v1/members/me/courses")
                        .header(MEMBER_ID_HEADER, 1L)
                        .param("cursor", "broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_INVALID_CURSOR"));
    }
}
