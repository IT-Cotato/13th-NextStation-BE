package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.CourseCopyRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseCreateRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseUpdateRequest;
import com.cotato.nextstation.domain.course.dto.response.CourseCreateResponse;
import com.cotato.nextstation.domain.course.dto.response.ExploreCourseListResponse;
import com.cotato.nextstation.domain.course.entity.CourseSort;
import com.cotato.nextstation.domain.course.dto.response.CourseUpdateResponse;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.service.command.CourseCommandService;
import com.cotato.nextstation.domain.course.service.command.CourseLikeCommandService;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
import com.cotato.nextstation.global.jwt.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CourseControllerTest {

    private static final String TOKEN = "access-token";

    @Autowired
    MockMvc mockMvc;

    // @WebMvcTest 슬라이스에 ObjectMapper 빈이 노출되지 않아 요청 직렬화용으로 직접 생성한다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    CourseCommandService courseCommandService;

    @MockitoBean
    CourseQueryService courseQueryService;

    @MockitoBean
    CourseLikeCommandService courseLikeCommandService;

    // WebConfig가 등록하는 JwtPrincipalArgumentResolver가 필요로 해서 @WebMvcTest 슬라이스에도 목이 필요하다
    @MockitoBean
    JwtProvider jwtProvider;

    @BeforeEach
    void authenticateAsMember1() {
        // 리졸버가 토큰에서 memberId를 꺼내므로, 토큰을 실은 요청은 1번 회원으로 인증된 것처럼 둔다
        given(jwtProvider.parseClaims(TOKEN)).willReturn(
                Jwts.claims().subject("1").add("purpose", "ACCESS").build());
    }

    @Test
    @DisplayName("코스 생성은 201과 courseId/name/createdAt을 반환한다")
    void createCourse_created() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest("보문역 코스", 1L, List.of(1L, 2L, 3L));
        given(courseCommandService.createCourse(eq(1L), any()))
                .willReturn(new CourseCreateResponse(1L, "보문역 코스", LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.courseId").value(1))
                .andExpect(jsonPath("$.data.name").value("보문역 코스"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @DisplayName("내 코스로 만들기는 201과 새로 만들어진 코스 정보를 반환한다")
    void copyCourse_created() throws Exception {
        CourseCopyRequest request = new CourseCopyRequest("내 보문역 코스", List.of(3L, 1L, 2L));
        given(courseCommandService.copyCourse(eq(1L), eq(9L), any()))
                .willReturn(new CourseCreateResponse(10L, "내 보문역 코스", LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/courses/{courseId}/copy", 9L)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                // 응답의 courseId는 원본(9)이 아니라 새로 만들어진 코스(10)여야 한다
                .andExpect(jsonPath("$.data.courseId").value(10))
                .andExpect(jsonPath("$.data.name").value("내 보문역 코스"));
    }

    @Test
    @DisplayName("본인이 만든 코스를 복사하면 400을 반환한다")
    void copyCourse_ownCourse() throws Exception {
        CourseCopyRequest request = new CourseCopyRequest("복사본", null);
        willThrow(new CustomException(CourseErrorCode.CANNOT_COPY_OWN_COURSE))
                .given(courseCommandService).copyCourse(eq(1L), eq(9L), any());

        mockMvc.perform(post("/api/v1/courses/{courseId}/copy", 9L)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CourseErrorCode.CANNOT_COPY_OWN_COURSE.getCode()));
    }

    @Test
    @DisplayName("코스 이름이 20자를 넘으면 검증 오류로 400을 반환한다")
    void copyCourse_nameTooLong() throws Exception {
        CourseCopyRequest request = new CourseCopyRequest("가".repeat(21), null);

        mockMvc.perform(post("/api/v1/courses/{courseId}/copy", 9L)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("장소가 3개 미만이면 검증 오류로 400을 반환한다")
    void createCourse_tooFewPlaces() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest("성수 코스", 100L, List.of(10L, 20L));

        mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.placeIds").exists());
    }

    @Test
    @DisplayName("이름과 장소 순서를 함께 수정하면 200과 courseId/name을 반환한다")
    void updateCourse_bothFields_success() throws Exception {
        given(courseCommandService.updateCourse(eq(1L), eq(1L), any()))
                .willReturn(new CourseUpdateResponse(1L, "나만의 보문역 코스"));

        mockMvc.perform(patch("/api/v1/courses/{courseId}", 1L)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CourseUpdateRequest("나만의 보문역 코스", List.of(3L, 1L, 4L, 2L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(1))
                .andExpect(jsonPath("$.data.name").value("나만의 보문역 코스"));
    }

    @Test
    @DisplayName("이름만 요청해도 200을 반환한다")
    void updateCourse_nameOnly_success() throws Exception {
        given(courseCommandService.updateCourse(eq(1L), eq(1L), any()))
                .willReturn(new CourseUpdateResponse(1L, "나만의 보문역 코스"));

        mockMvc.perform(patch("/api/v1/courses/{courseId}", 1L)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseUpdateRequest("나만의 보문역 코스", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("나만의 보문역 코스"));
    }

    @Test
    @DisplayName("장소 순서만 요청해도 200을 반환한다")
    void updateCourse_placeIdsOnly_success() throws Exception {
        given(courseCommandService.updateCourse(eq(1L), eq(1L), any()))
                .willReturn(new CourseUpdateResponse(1L, "보문역 코스"));

        mockMvc.perform(patch("/api/v1/courses/{courseId}", 1L)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseUpdateRequest(null, List.of(3L, 1L, 4L, 2L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(1));
    }

    @Test
    @DisplayName("이름과 장소 순서를 모두 생략하면 400을 반환한다")
    void updateCourse_bothMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/{courseId}", 1L)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseUpdateRequest(null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.anyFieldProvided").exists());
    }

    @Test
    @DisplayName("장소 순서에 빈 값이 섞이면 400과 함께 몇 번째 값이 문제인지 알려준다")
    void updateCourse_placeIdsWithNullElement() throws Exception {
        // List.of는 null을 담지 못해 Arrays.asList로 만든다
        CourseUpdateRequest request = new CourseUpdateRequest(null, Arrays.asList(3L, null, 2L));

        mockMvc.perform(patch("/api/v1/courses/{courseId}", 1L)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                // 다른 검증과 달리 키에 인덱스가 붙는다. 프론트가 이 형태를 기대해야 하므로 못박아 둔다
                .andExpect(jsonPath("$.reasons['placeIds[1]']").value("장소 ID는 비어 있을 수 없습니다."));
    }

    @Test
    @DisplayName("없는 코스를 수정하면 404를 반환한다")
    void updateCourse_notFound() throws Exception {
        given(courseCommandService.updateCourse(eq(1L), eq(1L), any()))
                .willThrow(new CustomException(CourseErrorCode.COURSE_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/courses/{courseId}", 1L)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseUpdateRequest("새 이름", null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_COURSE_NOT_FOUND"));
    }

    @Test
    @DisplayName("타인 소유 코스를 수정하면 403을 반환한다")
    void updateCourse_forbidden() throws Exception {
        given(courseCommandService.updateCourse(eq(1L), eq(1L), any()))
                .willThrow(new CustomException(CourseErrorCode.COURSE_FORBIDDEN));

        mockMvc.perform(patch("/api/v1/courses/{courseId}", 1L)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseUpdateRequest("새 이름", null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_403_COURSE_FORBIDDEN"));
    }

    @Test
    @DisplayName("코스 이름이 20자를 초과하면 400을 반환한다")
    void updateCourse_nameTooLong() throws Exception {
        String tooLongName = "가".repeat(21);

        mockMvc.perform(patch("/api/v1/courses/{courseId}", 1L)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseUpdateRequest(tooLongName, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.name").exists());
    }

    @Test
    @DisplayName("코스 이름이 공백이면 400을 반환한다")
    void updateCourse_nameBlank() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/{courseId}", 1L)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseUpdateRequest("   ", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reasons.nameNotBlank").exists());
    }

    @Test
    @DisplayName("장소가 3개 미만이면 400을 반환한다")
    void updateCourse_tooFewPlaces() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/{courseId}", 1L)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseUpdateRequest(null, List.of(3L, 1L)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.placeIds").exists());
    }

    @Test
    @DisplayName("장소 목록이 코스 구성과 다르면 400을 반환한다")
    void updateCourse_invalidPlaces() throws Exception {
        willThrow(new CustomException(CourseErrorCode.INVALID_COURSE_PLACES))
                .given(courseCommandService).updateCourse(eq(1L), eq(1L), any());

        mockMvc.perform(patch("/api/v1/courses/{courseId}", 1L)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseUpdateRequest(null, List.of(3L, 1L, 99L)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_INVALID_COURSE_PLACES"));
    }

    @Test
    @DisplayName("코스를 좋아요하면 201을 반환한다")
    void likeCourse_created() throws Exception {
        mockMvc.perform(post("/api/v1/courses/{courseId}/likes", 1L)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("이미 좋아요한 코스를 좋아요하면 409를 반환한다")
    void likeCourse_duplicate() throws Exception {
        willThrow(new CustomException(CourseErrorCode.DUPLICATE_COURSE_LIKE))
                .given(courseLikeCommandService).likeCourse(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/courses/{courseId}/likes", 1L)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_409_DUPLICATE_COURSE_LIKE"));
    }

    @Test
    @DisplayName("본인이 만든 코스를 좋아요하면 400을 반환한다")
    void likeCourse_ownCourse() throws Exception {
        willThrow(new CustomException(CourseErrorCode.CANNOT_LIKE_OWN_COURSE))
                .given(courseLikeCommandService).likeCourse(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/courses/{courseId}/likes", 1L)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_CANNOT_LIKE_OWN_COURSE"));
    }

    @Test
    @DisplayName("좋아요를 취소하면 200을 반환한다")
    void cancelCourseLike_success() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/{courseId}/likes", 1L)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("좋아요하지 않은 코스를 취소하면 404를 반환한다")
    void cancelCourseLike_notLiked() throws Exception {
        willThrow(new CustomException(CourseErrorCode.COURSE_LIKE_NOT_FOUND))
                .given(courseLikeCommandService).cancelLike(eq(1L), eq(1L));

        mockMvc.perform(delete("/api/v1/courses/{courseId}/likes", 1L)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_COURSE_LIKE_NOT_FOUND"));
    }

    @Test
    @DisplayName("본인 코스를 삭제하면 200을 반환한다")
    void deleteCourse_success() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/{courseId}", 1L)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("타인 코스를 삭제하면 403을 반환한다")
    void deleteCourse_forbidden() throws Exception {
        willThrow(new CustomException(CourseErrorCode.COURSE_DELETE_FORBIDDEN))
                .given(courseLikeCommandService).deleteCourse(eq(1L), eq(1L));

        mockMvc.perform(delete("/api/v1/courses/{courseId}", 1L)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_403_COURSE_DELETE_FORBIDDEN"));
    }

    @Test
    @DisplayName("토큰 없이 코스를 생성하면 401을 반환한다")
    void createCourse_withoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CourseCreateRequest("보문역 코스", 1L, List.of(1L, 2L, 3L)))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_401_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("둘러보기 목록은 토큰 없이도 200을 반환한다")
    void getExploreCourses_withoutToken() throws Exception {
        given(courseQueryService.getExploreCourses(isNull(), any(), any(), any(), any(), any(), any()))
                .willReturn(new ExploreCourseListResponse(List.of(), null, false));

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courses").isArray())
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("둘러보기 목록은 필터·검색·정렬을 그대로 서비스에 넘긴다")
    void getExploreCourses_passesParameters() throws Exception {
        given(courseQueryService.getExploreCourses(eq(1L), eq(2L), eq(123L), eq("신림"),
                eq(CourseSort.POPULAR), eq("cursor-value"), eq(5)))
                .willReturn(new ExploreCourseListResponse(List.of(), null, false));

        mockMvc.perform(get("/api/v1/courses")
                        .header("Authorization", "Bearer " + TOKEN)
                        .param("lineId", "2")
                        .param("stationId", "123")
                        .param("keyword", "신림")
                        .param("sort", "POPULAR")
                        .param("cursor", "cursor-value")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(courseQueryService).getExploreCourses(1L, 2L, 123L, "신림",
                CourseSort.POPULAR, "cursor-value", 5);
    }

    @Test
    @DisplayName("커서가 정렬과 맞지 않으면 400을 반환한다")
    void getExploreCourses_invalidCursor() throws Exception {
        given(courseQueryService.getExploreCourses(any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new CustomException(GlobalErrorCode.INVALID_CURSOR));

        mockMvc.perform(get("/api/v1/courses").param("cursor", "broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.INVALID_CURSOR.getCode()));
    }
}
