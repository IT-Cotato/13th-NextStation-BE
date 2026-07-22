package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.CourseSaveCancelRequest;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.service.command.CourseSaveCommandService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
