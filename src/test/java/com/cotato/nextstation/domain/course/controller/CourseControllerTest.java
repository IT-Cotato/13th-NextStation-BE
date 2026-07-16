package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.CourseCreateRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseNameUpdateRequest;
import com.cotato.nextstation.domain.course.dto.request.CoursePlaceOrderUpdateRequest;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseResponse;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.service.CourseCommandService;
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

    private CourseResponse sampleResponse(String name) {
        return new CourseResponse(1L, name, 100L, null, null, 0, 0, LocalDateTime.now(),
                List.of(new CoursePlaceResponse(10L, 1), new CoursePlaceResponse(20L, 2), new CoursePlaceResponse(30L, 3)));
    }

    @Test
    @DisplayName("코스 생성은 201과 생성된 코스를 반환한다")
    void createCourse_created() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest("성수 코스", 100L, null, List.of(10L, 20L, 30L));
        given(courseCommandService.createCourse(eq(1L), any())).willReturn(sampleResponse("성수 코스"));

        mockMvc.perform(post("/api/v1/courses")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("성수 코스"))
                .andExpect(jsonPath("$.data.places.length()").value(3));
    }

    @Test
    @DisplayName("장소가 3개 미만이면 검증 오류로 400을 반환한다")
    void createCourse_tooFewPlaces() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest("성수 코스", 100L, null, List.of(10L, 20L));

        mockMvc.perform(post("/api/v1/courses")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.placeIds").exists());
    }

    @Test
    @DisplayName("코스 이름을 수정하면 200을 반환한다")
    void updateCourseName_success() throws Exception {
        given(courseCommandService.updateCourseName(eq(1L), eq(1L), any())).willReturn(sampleResponse("새 이름"));

        mockMvc.perform(patch("/api/v1/courses/{courseId}/name", 1L)
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseNameUpdateRequest("새 이름"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("새 이름"));
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
    @DisplayName("코스 장소 순서를 수정하면 200을 반환한다")
    void updateCoursePlaceOrder_success() throws Exception {
        given(courseCommandService.updateCoursePlaceOrder(eq(1L), eq(1L), any())).willReturn(sampleResponse("성수 코스"));

        mockMvc.perform(patch("/api/v1/courses/{courseId}/places/order", 1L)
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CoursePlaceOrderUpdateRequest(List.of(30L, 10L, 20L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.places.length()").value(3));
    }
}
