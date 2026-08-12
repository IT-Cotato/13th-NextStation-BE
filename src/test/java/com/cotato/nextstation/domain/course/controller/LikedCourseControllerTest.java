package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.CourseLikeCancelAllRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseLikeCancelRequest;
import com.cotato.nextstation.domain.course.dto.response.CourseCardResponse;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.LineCode;
import com.cotato.nextstation.domain.course.dto.response.LikedCourseListResponse;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.service.command.CourseLikeCommandService;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
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

@WebMvcTest(LikedCourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LikedCourseControllerTest {

    private static final String TOKEN = "access-token";

    @Autowired
    MockMvc mockMvc;

    // @WebMvcTest 슬라이스에 ObjectMapper 빈이 노출되지 않아 요청 직렬화용으로 직접 생성한다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    CourseLikeCommandService courseLikeCommandService;

    @MockitoBean
    CourseQueryService courseQueryService;

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
    @DisplayName("좋아요 목록은 200과 코스 카드/다음 커서를 반환한다")
    void getLikedCourses_success() throws Exception {
        given(courseQueryService.getLikedCourses(1L, null, null)).willReturn(
                new LikedCourseListResponse(
                        List.of(new CourseCardResponse(7L, 10L, "보문역 환승여행 코스", 6L, "보문역",
                                new LineSummaryResponse(6L, "6호선", LineCode.LINE_6))),
                        "eyJpZCI6MjB9", true));

        mockMvc.perform(get("/api/v1/members/me/liked-courses").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courses[0].courseId").value(7))
                // 카드를 누르면 이 값으로 여행일지 상세를 연다
                .andExpect(jsonPath("$.data.courses[0].journalId").value(10))
                .andExpect(jsonPath("$.data.courses[0].name").value("보문역 환승여행 코스"))
                .andExpect(jsonPath("$.data.courses[0].stationName").value("보문역"))
                .andExpect(jsonPath("$.data.courses[0].line.id").value(6))
                .andExpect(jsonPath("$.data.courses[0].line.name").value("6호선"))
                .andExpect(jsonPath("$.data.courses[0].line.code").value("LINE_6"))
                .andExpect(jsonPath("$.data.nextCursor").value("eyJpZCI6MjB9"))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    @DisplayName("좋아요한 코스가 없으면 200과 빈 목록을 반환한다")
    void getLikedCourses_empty() throws Exception {
        given(courseQueryService.getLikedCourses(1L, null, null))
                .willReturn(new LikedCourseListResponse(List.of(), null, false));

        mockMvc.perform(get("/api/v1/members/me/liked-courses").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courses").isEmpty())
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    @DisplayName("커서와 size를 그대로 서비스에 전달한다")
    void getLikedCourses_passesParams() throws Exception {
        given(courseQueryService.getLikedCourses(1L, "abc", 5))
                .willReturn(new LikedCourseListResponse(List.of(), null, false));

        mockMvc.perform(get("/api/v1/members/me/liked-courses")
                        .header("Authorization", "Bearer " + TOKEN)
                        .param("cursor", "abc")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(courseQueryService).getLikedCourses(1L, "abc", 5);
    }

    @Test
    @DisplayName("size가 허용 범위를 벗어나면 400을 반환한다")
    void getLikedCourses_invalidSize() throws Exception {
        given(courseQueryService.getLikedCourses(1L, null, 100))
                .willThrow(new CustomException(GlobalErrorCode.INVALID_PAGE_SIZE));

        mockMvc.perform(get("/api/v1/members/me/liked-courses")
                        .header("Authorization", "Bearer " + TOKEN)
                        .param("size", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_INVALID_PAGE_SIZE"));
    }

    @Test
    @DisplayName("여러 좋아요를 한 번에 취소하면 200을 반환한다")
    void cancelCourseLikes_success() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me/liked-courses")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseLikeCancelRequest(List.of(1L, 2L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("취소할 코스를 선택하지 않으면 400을 반환한다")
    void cancelCourseLikes_empty() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me/liked-courses")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseLikeCancelRequest(List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.courseIds").exists());
    }

    @Test
    @DisplayName("전체 취소는 본문 없이 호출해도 200을 반환한다")
    void cancelAllCourseLikes_success() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me/liked-courses/all")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(courseLikeCommandService).cancelAllLikes(1L, null);
    }

    @Test
    @DisplayName("전체 취소에서 해제한 코스는 제외 목록으로 전달된다")
    void cancelAllCourseLikes_withExceptions() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me/liked-courses/all")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseLikeCancelAllRequest(List.of(3L, 7L)))))
                .andExpect(status().isOk());

        verify(courseLikeCommandService).cancelAllLikes(1L, List.of(3L, 7L));
    }

    @Test
    @DisplayName("취소할 좋아요가 없으면 404를 반환한다")
    void cancelAllCourseLikes_nothingToCancel() throws Exception {
        willThrow(new CustomException(CourseErrorCode.COURSE_LIKE_NOT_FOUND))
                .given(courseLikeCommandService).cancelAllLikes(eq(1L), any());

        mockMvc.perform(delete("/api/v1/members/me/liked-courses/all")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_COURSE_LIKE_NOT_FOUND"));
    }

    @Test
    @DisplayName("선택한 코스가 모두 좋아요돼 있지 않으면 404를 반환한다")
    void cancelCourseLikes_noneLiked() throws Exception {
        willThrow(new CustomException(CourseErrorCode.COURSE_LIKE_NOT_FOUND))
                .given(courseLikeCommandService).cancelLikes(eq(1L), any());

        mockMvc.perform(delete("/api/v1/members/me/liked-courses")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CourseLikeCancelRequest(List.of(1L)))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_COURSE_LIKE_NOT_FOUND"));
    }

    @Test
    @DisplayName("토큰 없이 좋아요 코스 목록을 조회하면 401을 반환한다")
    void getLikedCourses_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/liked-courses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_401_UNAUTHORIZED"));
    }
}
