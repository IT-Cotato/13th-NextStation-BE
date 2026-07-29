package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.MyCourseDeleteRequest;
import com.cotato.nextstation.domain.course.dto.response.MyCourseCardResponse;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.service.command.CourseLikeCommandService;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.LineCode;
import com.cotato.nextstation.domain.course.dto.response.MyCourseListResponse;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import com.cotato.nextstation.global.jwt.JwtProvider;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    // @WebMvcTest 슬라이스에 ObjectMapper 빈이 노출되지 않아 요청 직렬화용으로 직접 생성한다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    CourseQueryService courseQueryService;

    @MockitoBean
    CourseLikeCommandService courseLikeCommandService;

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
                        List.of(new MyCourseCardResponse(7L, "보문역 환승여행 코스", 6L, "보문역",
                                new LineSummaryResponse(6L, "6호선", LineCode.LINE_6), true)),
                        null, false));

        mockMvc.perform(get("/api/v1/members/me/courses").header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableLines[0].id").value(1))
                .andExpect(jsonPath("$.data.availableLines[0].name").value("1호선"))
                .andExpect(jsonPath("$.data.availableLines[1].name").value("6호선"))
                .andExpect(jsonPath("$.data.availableLines[1].code").value("LINE_6"))
                .andExpect(jsonPath("$.data.courses[0].courseId").value(7))
                .andExpect(jsonPath("$.data.courses[0].stationName").value("보문역"))
                .andExpect(jsonPath("$.data.courses[0].isCompleted").value(true))
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

    @Test
    @DisplayName("선택한 코스를 다중 삭제하면 200을 반환한다")
    void deleteMyCourses_success() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me/courses")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MyCourseDeleteRequest(List.of(1L, 2L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(courseLikeCommandService).deleteCourses(1L, List.of(1L, 2L));
    }

    @Test
    @DisplayName("삭제할 코스를 선택하지 않으면 400을 반환한다")
    void deleteMyCourses_empty() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me/courses")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MyCourseDeleteRequest(List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.courseIds").exists());
    }

    @Test
    @DisplayName("선택한 코스가 모두 본인 것이 아니거나 없으면 404를 반환한다")
    void deleteMyCourses_notFound() throws Exception {
        willThrow(new CustomException(CourseErrorCode.COURSE_NOT_FOUND))
                .given(courseLikeCommandService).deleteCourses(eq(1L), any());

        mockMvc.perform(delete("/api/v1/members/me/courses")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MyCourseDeleteRequest(List.of(1L)))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_COURSE_NOT_FOUND"));
    }
}
