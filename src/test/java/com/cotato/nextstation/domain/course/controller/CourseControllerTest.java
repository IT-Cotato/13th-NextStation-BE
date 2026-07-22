package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.CourseCreateRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseNameUpdateRequest;
import com.cotato.nextstation.domain.course.dto.request.CoursePlaceOrderUpdateRequest;
import com.cotato.nextstation.domain.course.dto.response.CourseCreateResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseNameResponse;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.service.command.CourseCommandService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CourseControllerTest {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    @Autowired
    MockMvc mockMvc;

    // @WebMvcTest 슬라이스에 ObjectMapper 빈이 노출되지 않아 요청 직렬화용으로 직접 생성한다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    CourseCommandService courseCommandService;

    @MockitoBean
    CourseSaveCommandService courseSaveCommandService;

    @Test
    @DisplayName("코스 생성은 201과 courseId/name/createdAt을 반환한다")
    void createCourse_created() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest("보문역 코스", 1L, List.of(1L, 2L, 3L));
        given(courseCommandService.createCourse(eq(1L), any()))
                .willReturn(new CourseCreateResponse(1L, "보문역 코스", LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/courses")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.courseId").value(1))
                .andExpect(jsonPath("$.data.name").value("보문역 코스"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @DisplayName("장소가 3개 미만이면 검증 오류로 400을 반환한다")
    void createCourse_tooFewPlaces() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest("성수 코스", 100L, List.of(10L, 20L));

        mockMvc.perform(post("/api/v1/courses")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.placeIds").exists());
    }

    @Test
    @DisplayName("코스 이름을 수정하면 200과 courseId/name을 반환한다")
    void updateCourseName_success() throws Exception {
        given(courseCommandService.updateCourseName(eq(1L), eq(1L), any()))
                .willReturn(new CourseNameResponse(1L, "나만의 보문역 코스"));

        mockMvc.perform(patch("/api/v1/courses/{courseId}/name", 1L)
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseNameUpdateRequest("나만의 보문역 코스"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(1))
                .andExpect(jsonPath("$.data.name").value("나만의 보문역 코스"));
    }

    @Test
    @DisplayName("없는 코스의 이름을 수정하면 404를 반환한다")
    void updateCourseName_notFound() throws Exception {
        given(courseCommandService.updateCourseName(eq(1L), eq(1L), any()))
                .willThrow(new CustomException(CourseErrorCode.COURSE_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/courses/{courseId}/name", 1L)
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseNameUpdateRequest("새 이름"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_COURSE_NOT_FOUND"));
    }

    @Test
    @DisplayName("타인 소유 코스의 이름을 수정하면 403을 반환한다")
    void updateCourseName_forbidden() throws Exception {
        given(courseCommandService.updateCourseName(eq(1L), eq(1L), any()))
                .willThrow(new CustomException(CourseErrorCode.COURSE_FORBIDDEN));

        mockMvc.perform(patch("/api/v1/courses/{courseId}/name", 1L)
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseNameUpdateRequest("새 이름"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_403_COURSE_FORBIDDEN"));
    }

    @Test
    @DisplayName("코스 이름이 20자를 초과하면 400을 반환한다")
    void updateCourseName_tooLong() throws Exception {
        String tooLongName = "가".repeat(21);

        mockMvc.perform(patch("/api/v1/courses/{courseId}/name", 1L)
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseNameUpdateRequest(tooLongName))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.name").exists());
    }

    @Test
    @DisplayName("코스 이름이 비어 있으면 400을 반환한다")
    void updateCourseName_blank() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/{courseId}/name", 1L)
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseNameUpdateRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reasons.name").exists());
    }

    @Test
    @DisplayName("코스 장소 순서를 수정하면 200과 데이터 없는 응답을 반환한다")
    void updateCoursePlaceOrder_success() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/{courseId}/places/order", 1L)
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CoursePlaceOrderUpdateRequest(List.of(3L, 1L, 4L, 2L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("장소 목록이 코스 구성과 다르면 400을 반환한다")
    void updateCoursePlaceOrder_invalidPlaces() throws Exception {
        willThrow(new CustomException(CourseErrorCode.INVALID_COURSE_PLACES))
                .given(courseCommandService).updateCoursePlaceOrder(eq(1L), eq(1L), any());

        mockMvc.perform(patch("/api/v1/courses/{courseId}/places/order", 1L)
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CoursePlaceOrderUpdateRequest(List.of(3L, 1L, 99L)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_INVALID_COURSE_PLACES"));
    }

    @Test
    @DisplayName("코스를 스크랩하면 201을 반환한다")
    void saveCourse_created() throws Exception {
        mockMvc.perform(post("/api/v1/courses/{courseId}/saves", 1L)
                        .header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("이미 저장한 코스를 스크랩하면 409를 반환한다")
    void saveCourse_duplicate() throws Exception {
        willThrow(new CustomException(CourseErrorCode.DUPLICATE_COURSE_SAVE))
                .given(courseSaveCommandService).saveCourse(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/courses/{courseId}/saves", 1L)
                        .header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_409_DUPLICATE_COURSE_SAVE"));
    }

    @Test
    @DisplayName("스크랩을 취소하면 200을 반환한다")
    void cancelCourseSave_success() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/{courseId}/saves", 1L)
                        .header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("저장하지 않은 코스를 취소하면 404를 반환한다")
    void cancelCourseSave_notSaved() throws Exception {
        willThrow(new CustomException(CourseErrorCode.COURSE_SAVE_NOT_FOUND))
                .given(courseSaveCommandService).cancelSave(eq(1L), eq(1L));

        mockMvc.perform(delete("/api/v1/courses/{courseId}/saves", 1L)
                        .header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_COURSE_SAVE_NOT_FOUND"));
    }

    @Test
    @DisplayName("본인 코스를 삭제하면 200을 반환한다")
    void deleteCourse_success() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/{courseId}", 1L)
                        .header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("타인 코스를 삭제하면 403을 반환한다")
    void deleteCourse_forbidden() throws Exception {
        willThrow(new CustomException(CourseErrorCode.COURSE_DELETE_FORBIDDEN))
                .given(courseSaveCommandService).deleteCourse(eq(1L), eq(1L));

        mockMvc.perform(delete("/api/v1/courses/{courseId}", 1L)
                        .header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_403_COURSE_DELETE_FORBIDDEN"));
    }
}
