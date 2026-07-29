package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.ExploreCourseCondition;
import com.cotato.nextstation.domain.course.dto.response.ConceptTourResponse;
import com.cotato.nextstation.domain.course.dto.response.ExploreCourseListResponse;
import com.cotato.nextstation.domain.course.entity.CourseSort;
import com.cotato.nextstation.domain.course.service.query.ConceptTourQueryService;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConceptTourController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ConceptTourControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ConceptTourQueryService conceptTourQueryService;

    @MockitoBean
    CourseQueryService courseQueryService;

    // WebConfig가 등록하는 JwtPrincipalArgumentResolver가 필요로 해서 @WebMvcTest 슬라이스에도 목이 필요하다
    @MockitoBean
    JwtProvider jwtProvider;

    @Test
    @DisplayName("컨셉 목록은 코스 수와 함께 반환한다")
    void getConceptTours_success() throws Exception {
        given(conceptTourQueryService.getConceptTours()).willReturn(List.of(
                new ConceptTourResponse(1L, "문구 투어", "작은 문구점과 책방을 찾아가는 코스", 18)));

        mockMvc.perform(get("/api/v1/concept-tours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].conceptTourId").value(1))
                .andExpect(jsonPath("$.data[0].name").value("문구 투어"))
                .andExpect(jsonPath("$.data[0].courseCount").value(18));
    }

    @Test
    @DisplayName("컨셉 목록은 토큰 없이도 조회된다")
    void getConceptTours_withoutToken() throws Exception {
        given(conceptTourQueryService.getConceptTours()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/concept-tours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("컨셉별 코스는 컨셉 조건만 걸어 둘러보기 조회를 재사용한다")
    void getConceptTourCourses_usesConceptCondition() throws Exception {
        given(courseQueryService.getExploreCourses(isNull(), any(), any(), any(), any()))
                .willReturn(new ExploreCourseListResponse(List.of(), null, false));

        mockMvc.perform(get("/api/v1/concept-tours/{conceptTourId}/courses", 1L)
                        .param("sort", "POPULAR")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(courseQueryService).getExploreCourses(
                null, ExploreCourseCondition.ofConceptTour(1L), CourseSort.POPULAR, null, 5);
    }

    @Test
    @DisplayName("정렬을 생략하면 서비스가 기본값을 정하도록 null을 넘긴다")
    void getConceptTourCourses_defaultSort() throws Exception {
        given(courseQueryService.getExploreCourses(any(), any(), any(), any(), any()))
                .willReturn(new ExploreCourseListResponse(List.of(), null, false));

        mockMvc.perform(get("/api/v1/concept-tours/{conceptTourId}/courses", 1L))
                .andExpect(status().isOk());

        verify(courseQueryService).getExploreCourses(
                isNull(), eq(ExploreCourseCondition.ofConceptTour(1L)), isNull(), isNull(), isNull());
    }
}
