package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.CourseSaveCancelAllRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseSaveCancelRequest;
import com.cotato.nextstation.domain.course.dto.response.CourseCardResponse;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.LineCode;
import com.cotato.nextstation.domain.course.dto.response.SavedCourseListResponse;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.service.command.CourseSaveCommandService;
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

@WebMvcTest(SavedCourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SavedCourseControllerTest {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    @Autowired
    MockMvc mockMvc;

    // @WebMvcTest 슬라이스에 ObjectMapper 빈이 노출되지 않아 요청 직렬화용으로 직접 생성한다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    CourseSaveCommandService courseSaveCommandService;

    @MockitoBean
    CourseQueryService courseQueryService;

    // WebConfig가 등록하는 JwtPrincipalArgumentResolver가 필요로 해서 @WebMvcTest 슬라이스에도 목이 필요하다
    @MockitoBean
    JwtProvider jwtProvider;

    @Test
    @DisplayName("스크랩 목록은 200과 코스 카드/다음 커서를 반환한다")
    void getSavedCourses_success() throws Exception {
        given(courseQueryService.getSavedCourses(1L, null, null)).willReturn(
                new SavedCourseListResponse(
                        List.of(new CourseCardResponse(7L, "보문역 환승여행 코스", 6L, "보문역",
                                new LineSummaryResponse(6L, "6호선", LineCode.LINE_6))),
                        "eyJpZCI6MjB9", true));

        mockMvc.perform(get("/api/v1/members/me/saved-courses").header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courses[0].courseId").value(7))
                .andExpect(jsonPath("$.data.courses[0].name").value("보문역 환승여행 코스"))
                .andExpect(jsonPath("$.data.courses[0].stationName").value("보문역"))
                .andExpect(jsonPath("$.data.courses[0].line.id").value(6))
                .andExpect(jsonPath("$.data.courses[0].line.name").value("6호선"))
                .andExpect(jsonPath("$.data.courses[0].line.code").value("LINE_6"))
                .andExpect(jsonPath("$.data.nextCursor").value("eyJpZCI6MjB9"))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    @DisplayName("스크랩한 코스가 없으면 200과 빈 목록을 반환한다")
    void getSavedCourses_empty() throws Exception {
        given(courseQueryService.getSavedCourses(1L, null, null))
                .willReturn(new SavedCourseListResponse(List.of(), null, false));

        mockMvc.perform(get("/api/v1/members/me/saved-courses").header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courses").isEmpty())
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("커서와 size를 그대로 서비스에 전달한다")
    void getSavedCourses_passesParams() throws Exception {
        given(courseQueryService.getSavedCourses(1L, "abc", 5))
                .willReturn(new SavedCourseListResponse(List.of(), null, false));

        mockMvc.perform(get("/api/v1/members/me/saved-courses")
                        .header(MEMBER_ID_HEADER, 1L)
                        .param("cursor", "abc")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(courseQueryService).getSavedCourses(1L, "abc", 5);
    }

    @Test
    @DisplayName("size가 허용 범위를 벗어나면 400을 반환한다")
    void getSavedCourses_invalidSize() throws Exception {
        given(courseQueryService.getSavedCourses(1L, null, 100))
                .willThrow(new CustomException(GlobalErrorCode.INVALID_PAGE_SIZE));

        mockMvc.perform(get("/api/v1/members/me/saved-courses")
                        .header(MEMBER_ID_HEADER, 1L)
                        .param("size", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_INVALID_PAGE_SIZE"));
    }

    @Test
    @DisplayName("여러 스크랩을 한 번에 취소하면 200을 반환한다")
    void cancelCourseSaves_success() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me/saved-courses")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseSaveCancelRequest(List.of(1L, 2L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("취소할 코스를 선택하지 않으면 400을 반환한다")
    void cancelCourseSaves_empty() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me/saved-courses")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseSaveCancelRequest(List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.courseIds").exists());
    }

    @Test
    @DisplayName("전체 취소는 본문 없이 호출해도 200을 반환한다")
    void cancelAllCourseSaves_success() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me/saved-courses/all")
                        .header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(courseSaveCommandService).cancelAllSaves(1L, null);
    }

    @Test
    @DisplayName("전체 취소에서 해제한 코스는 제외 목록으로 전달된다")
    void cancelAllCourseSaves_withExceptions() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me/saved-courses/all")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseSaveCancelAllRequest(List.of(3L, 7L)))))
                .andExpect(status().isOk());

        verify(courseSaveCommandService).cancelAllSaves(1L, List.of(3L, 7L));
    }

    @Test
    @DisplayName("취소할 스크랩이 없으면 404를 반환한다")
    void cancelAllCourseSaves_nothingToCancel() throws Exception {
        willThrow(new CustomException(CourseErrorCode.COURSE_SAVE_NOT_FOUND))
                .given(courseSaveCommandService).cancelAllSaves(eq(1L), any());

        mockMvc.perform(delete("/api/v1/members/me/saved-courses/all")
                        .header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_COURSE_SAVE_NOT_FOUND"));
    }

    @Test
    @DisplayName("선택한 코스가 모두 저장돼 있지 않으면 404를 반환한다")
    void cancelCourseSaves_noneSaved() throws Exception {
        willThrow(new CustomException(CourseErrorCode.COURSE_SAVE_NOT_FOUND))
                .given(courseSaveCommandService).cancelSaves(eq(1L), any());

        mockMvc.perform(delete("/api/v1/members/me/saved-courses")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseSaveCancelRequest(List.of(1L)))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_COURSE_SAVE_NOT_FOUND"));
    }
}
