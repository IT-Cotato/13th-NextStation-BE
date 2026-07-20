package com.cotato.nextstation.domain.recommendation.controller;

import com.cotato.nextstation.domain.recommendation.dto.response.CoursePreviewPlaceResponse;
import com.cotato.nextstation.domain.recommendation.dto.response.CoursePreviewResponse;
import com.cotato.nextstation.domain.recommendation.dto.response.RandomRecommendationResponse;
import com.cotato.nextstation.domain.recommendation.dto.response.RecommendedStationResponse;
import com.cotato.nextstation.domain.recommendation.exception.RecommendationErrorCode;
import com.cotato.nextstation.domain.recommendation.service.command.RecommendationCommandService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RandomController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RandomControllerTest {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RecommendationCommandService recommendationCommandService;

    private RandomRecommendationResponse sampleResponse() {
        return new RandomRecommendationResponse(
                new RecommendedStationResponse(10L, "제기동역", "제기동역 소개", "1호선"),
                new CoursePreviewResponse("제기동역 환승여행 코스", List.of(
                        new CoursePreviewPlaceResponse(100L, "경동시장", "설명", "CULTURE", "문화공간", "img", 127.0, 37.5)
                ))
        );
    }

    @Test
    @DisplayName("로그인 뽑기는 헤더의 memberId로 호출되고 200과 역/코스를 반환한다")
    void drawRandom_withMember() throws Exception {
        given(recommendationCommandService.drawRandom(eq(1L))).willReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/random").header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.station.stationName").value("제기동역"))
                .andExpect(jsonPath("$.data.station.lineName").value("1호선"))
                .andExpect(jsonPath("$.data.course.name").value("제기동역 환승여행 코스"))
                .andExpect(jsonPath("$.data.course.places[0].categoryCode").value("CULTURE"));
    }

    @Test
    @DisplayName("헤더 없는 비로그인 뽑기는 memberId null로 호출되고 200을 반환한다")
    void drawRandom_anonymous() throws Exception {
        given(recommendationCommandService.drawRandom(isNull())).willReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.station.stationId").value(10));
    }

    @Test
    @DisplayName("뽑기 대상 역이 없으면 404를 반환한다")
    void drawRandom_noDrawableStation() throws Exception {
        given(recommendationCommandService.drawRandom(any()))
                .willThrow(new CustomException(RecommendationErrorCode.NO_DRAWABLE_STATION));

        mockMvc.perform(post("/api/v1/random"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_NO_DRAWABLE_STATION"));
    }
}
