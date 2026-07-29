package com.cotato.nextstation.domain.recommendation.controller;

import com.cotato.nextstation.domain.recommendation.dto.response.CoursePreviewPlaceResponse;
import com.cotato.nextstation.domain.recommendation.dto.response.CoursePreviewResponse;
import com.cotato.nextstation.domain.recommendation.dto.response.RandomRecommendationResponse;
import com.cotato.nextstation.domain.recommendation.dto.response.RecommendedStationResponse;
import com.cotato.nextstation.domain.recommendation.exception.RecommendationErrorCode;
import com.cotato.nextstation.domain.recommendation.service.command.RecommendationCommandService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
import com.cotato.nextstation.global.jwt.JwtProvider;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.LineCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RandomController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RandomControllerTest {

    private static final String TOKEN = "access-token";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RecommendationCommandService recommendationCommandService;

    // WebConfig가 등록하는 JwtPrincipalArgumentResolver가 필요로 해서 @WebMvcTest 슬라이스에도 목이 필요하다
    @MockitoBean
    JwtProvider jwtProvider;

    @BeforeEach
    void authenticateAsMember1() {
        // 리졸버가 토큰에서 memberId를 꺼내므로, 토큰을 실은 요청은 1번 회원으로 인증된 것처럼 둔다
        given(jwtProvider.parseClaims(TOKEN)).willReturn(
                Jwts.claims().subject("1").add("purpose", "ACCESS").build());
    }

    private RandomRecommendationResponse sampleResponse() {
        return new RandomRecommendationResponse(
                new RecommendedStationResponse(10L, "보문역", "보문역 소개",
                        List.of("성북천을 따라 가볍게 산책하기", "보문동 골목과 생활 상권 둘러보기"),
                        List.of(new LineSummaryResponse(6L, "6호선", LineCode.LINE_6),
                                new LineSummaryResponse(21L, "우이신설선", LineCode.UI_SINSEOL))),
                new CoursePreviewResponse("보문역 환승여행 코스", List.of(
                        new CoursePreviewPlaceResponse(100L, "성북천", "설명", "CULTURE", "문화공간", "img", 127.0, 37.5)
                ))
        );
    }

    @Test
    @DisplayName("로그인 뽑기는 토큰의 memberId로 호출되고 200과 역/코스를 반환한다")
    void drawRandom_withMember() throws Exception {
        given(recommendationCommandService.drawRandom(eq(1L))).willReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/random").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.station.stationName").value("보문역"))
                .andExpect(jsonPath("$.data.station.todos.length()").value(2))
                .andExpect(jsonPath("$.data.station.todos[0]").value("성북천을 따라 가볍게 산책하기"))
                .andExpect(jsonPath("$.data.station.lines.length()").value(2))
                .andExpect(jsonPath("$.data.station.lines[0].id").value(6))
                .andExpect(jsonPath("$.data.station.lines[0].name").value("6호선"))
                .andExpect(jsonPath("$.data.station.lines[0].code").value("LINE_6"))
                .andExpect(jsonPath("$.data.station.lines[1].name").value("우이신설선"))
                .andExpect(jsonPath("$.data.course.name").value("보문역 환승여행 코스"))
                .andExpect(jsonPath("$.data.course.places[0].categoryCode").value("CULTURE"));
    }

    @Test
    @DisplayName("노선·할 일이 없는 역은 필드가 생략되지 않고 빈 배열로 내려간다")
    void drawRandom_emptyLinesAndTodos() throws Exception {
        RandomRecommendationResponse response = new RandomRecommendationResponse(
                new RecommendedStationResponse(10L, "보문역", "보문역 소개", List.of(), List.of()),
                new CoursePreviewResponse("보문역 환승여행 코스", List.of()));
        given(recommendationCommandService.drawRandom(eq(1L))).willReturn(response);

        mockMvc.perform(post("/api/v1/random").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.station.lines").isArray())
                .andExpect(jsonPath("$.data.station.lines").isEmpty())
                .andExpect(jsonPath("$.data.station.todos").isArray())
                .andExpect(jsonPath("$.data.station.todos").isEmpty());
    }

    @Test
    @DisplayName("토큰 없는 비로그인 뽑기는 memberId null로 호출되고 200을 반환한다")
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

    @Test
    @DisplayName("만료·위조된 토큰을 보내면 비로그인으로 넘기지 않고 401을 반환한다")
    void drawRandom_invalidToken() throws Exception {
        // given: 토큰을 보냈다는 건 로그인한 줄 알고 있다는 뜻이라, 조용히 비로그인 처리하면 안 된다
        given(jwtProvider.parseClaims("broken-token")).willThrow(new MalformedJwtException("broken"));

        // when & then
        mockMvc.perform(post("/api/v1/random")
                        .header("Authorization", "Bearer broken-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_401_INVALID_TOKEN"));
        verify(recommendationCommandService, never()).drawRandom(any());
    }

    @Test
    @DisplayName("Bearer 형식이 아닌 Authorization 헤더는 비로그인으로 넘기지 않고 401을 반환한다")
    void drawRandom_nonBearerHeader() throws Exception {
        // given: 자격 증명을 실어 보냈다는 뜻이라 익명 요청으로 취급하면 안 된다

        // when & then
        mockMvc.perform(post("/api/v1/random")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_401_INVALID_TOKEN"));
        verify(recommendationCommandService, never()).drawRandom(any());
    }

    @Test
    @DisplayName("값이 빈 Authorization 헤더는 보내지 않은 것과 같게 보고 비로그인으로 처리한다")
    void drawRandom_blankHeader() throws Exception {
        // given: 로그아웃 상태에서 빈 헤더를 실어 보내는 클라이언트가 있어도 둘러보기가 막히면 안 된다
        given(recommendationCommandService.drawRandom(isNull())).willReturn(sampleResponse());

        // when & then
        mockMvc.perform(post("/api/v1/random")
                        .header("Authorization", "  "))
                .andExpect(status().isOk());
    }
}
