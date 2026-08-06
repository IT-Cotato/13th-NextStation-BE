package com.cotato.nextstation.domain.station.controller;

import com.cotato.nextstation.domain.station.dto.response.StationPlaceCategoryResponse;
import com.cotato.nextstation.domain.station.dto.response.StationPlaceResponse;
import com.cotato.nextstation.domain.station.dto.response.StationPlacesResponse;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.exception.StationErrorCode;
import com.cotato.nextstation.domain.station.service.query.StationQueryService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
import com.cotato.nextstation.global.jwt.JwtProvider;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.LineCode;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    StationQueryService stationQueryService;

    // WebConfig가 등록하는 JwtPrincipalArgumentResolver가 필요로 해서 @WebMvcTest 슬라이스에도 목이 필요하다
    @MockitoBean
    JwtProvider jwtProvider;

    @Test
    @DisplayName("역 검색은 200과 역/노선 목록을 반환한다")
    void searchStations_success() throws Exception {
        given(stationQueryService.searchByName("왕십리역")).willReturn(List.of(
                new StationSummaryResponse(42L, "왕십리역", List.of(
                        new LineSummaryResponse(2L, "2호선", LineCode.LINE_2),
                        new LineSummaryResponse(5L, "5호선", LineCode.LINE_5)))));

        mockMvc.perform(get("/api/v1/stations").param("keyword", "왕십리역"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stationId").value(42))
                .andExpect(jsonPath("$.data[0].stationName").value("왕십리역"))
                .andExpect(jsonPath("$.data[0].lines[0].name").value("2호선"))
                .andExpect(jsonPath("$.data[0].lines[0].code").value("LINE_2"))
                .andExpect(jsonPath("$.data[0].lines[1].name").value("5호선"));
    }

    @Test
    @DisplayName("결과가 없으면 200과 빈 목록을 반환한다")
    void searchStations_empty() throws Exception {
        given(stationQueryService.searchByName("없는역")).willReturn(List.of());

        mockMvc.perform(get("/api/v1/stations").param("keyword", "없는역"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // 랜덤뽑기·맞춤추천 결과 화면이 비로그인으로도 접근 가능해, 그 다음 단계인 출발역 검색도 비로그인 접근이 유지돼야 한다(#147).
    @Test
    @DisplayName("Authorization 헤더 없이도 200을 반환한다 (비로그인 접근 유지 확정, #147)")
    void searchStations_noAuthHeaderRequired() throws Exception {
        given(stationQueryService.searchByName("왕십리역")).willReturn(List.of());

        mockMvc.perform(get("/api/v1/stations").param("keyword", "왕십리역"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("역별 장소 목록은 200과 카테고리별 장소를 반환한다")
    void getStationPlaces_success() throws Exception {
        given(stationQueryService.getStationPlaces(6L, null)).willReturn(
                new StationPlacesResponse(6L, "보문역", "성북천을 따라 걷기 좋은 역",
                        new LineSummaryResponse(6L, "6호선", LineCode.LINE_6),
                        List.of(new LineSummaryResponse(6L, "6호선", LineCode.LINE_6),
                                new LineSummaryResponse(18L, "우이신설선", LineCode.UI_SINSEOL)), List.of("LOCAL_EXPLORE", "NATURE"),
                        "보문역 환승여행 코스", List.of(
                        new StationPlaceCategoryResponse("CULTURE", "문화공간", List.of(
                                new StationPlaceResponse(11L, "보문숲길도서관", "동네 도서관", "img", 127.0, 37.5)))
                )));

        mockMvc.perform(get("/api/v1/stations/{stationId}/places", 6L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stationName").value("보문역"))
                .andExpect(jsonPath("$.data.description").value("성북천을 따라 걷기 좋은 역"))
                .andExpect(jsonPath("$.data.line.name").value("6호선"))
                .andExpect(jsonPath("$.data.line.code").value("LINE_6"))
                .andExpect(jsonPath("$.data.lines[1].name").value("우이신설선"))
                .andExpect(jsonPath("$.data.tags[0]").value("LOCAL_EXPLORE"))
                .andExpect(jsonPath("$.data.defaultCourseName").value("보문역 환승여행 코스"))
                .andExpect(jsonPath("$.data.categories[0].categoryCode").value("CULTURE"))
                .andExpect(jsonPath("$.data.categories[0].places[0].placeName").value("보문숲길도서관"));
    }

    @Test
    @DisplayName("장소가 없는 역은 200과 빈 카테고리 목록을 반환한다")
    void getStationPlaces_noPlaces() throws Exception {
        given(stationQueryService.getStationPlaces(300L, null))
                .willReturn(new StationPlacesResponse(300L, "서울역", null, null,
                        List.of(new LineSummaryResponse(1L, "1호선", LineCode.LINE_1), new LineSummaryResponse(4L, "4호선", LineCode.LINE_4)), List.of(), "서울역 환승여행 코스", List.of()));

        mockMvc.perform(get("/api/v1/stations/{stationId}/places", 300L))
                .andExpect(status().isOk())
                // 대표 호선이 없어도 line 필드는 생략되지 않고 JSON null로 존재해야 한다
                .andExpect(jsonPath("$.data.line").hasJsonPath())
                .andExpect(jsonPath("$.data.line").value(nullValue()))
                .andExpect(jsonPath("$.data.categories").isEmpty());
    }

    // 랜덤뽑기·맞춤추천 결과 화면이 비로그인으로도 접근 가능해, 그 다음 단계인 코스 만들기 후보 조회도 비로그인 접근이 유지돼야 한다(#147).
    @Test
    @DisplayName("Authorization 헤더 없이도 200을 반환한다 (비로그인 접근 유지 확정, #147)")
    void getStationPlaces_noAuthHeaderRequired() throws Exception {
        given(stationQueryService.getStationPlaces(6L, null))
                .willReturn(new StationPlacesResponse(6L, "보문역", null, null, List.of(), List.of(), "보문역 환승여행 코스", List.of()));

        mockMvc.perform(get("/api/v1/stations/{stationId}/places", 6L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("존재하지 않는 역이면 404를 반환한다")
    void getStationPlaces_notFound() throws Exception {
        given(stationQueryService.getStationPlaces(999L, null))
                .willThrow(new CustomException(StationErrorCode.STATION_NOT_FOUND));

        mockMvc.perform(get("/api/v1/stations/{stationId}/places", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_STATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("존재하지 않는 여행 스타일이면 400을 반환하고 조회하지 않는다")
    void getStationPlaces_invalidTravelStyle() throws Exception {
        mockMvc.perform(get("/api/v1/stations/{stationId}/places", 6L)
                        .param("travelStyles", "NOT_A_TAG"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.travelStyles").value("존재하지 않는 여행 스타일입니다."));
        verify(stationQueryService, never()).getStationPlaces(any(), any());
    }

    @Test
    @DisplayName("여행 스타일이 중복되면 400을 반환하고 조회하지 않는다")
    void getStationPlaces_duplicateTravelStyle() throws Exception {
        mockMvc.perform(get("/api/v1/stations/{stationId}/places", 6L)
                        .param("travelStyles", "NATURE", "NATURE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.travelStyles").value("중복된 값은 넣을 수 없습니다."));
        verify(stationQueryService, never()).getStationPlaces(any(), any());
    }

    @Test
    @DisplayName("여행 스타일 검증이 역 조회보다 먼저라 없는 역이어도 400을 반환한다")
    void getStationPlaces_invalidTravelStyleTakesPrecedenceOverMissingStation() throws Exception {
        // 검증이 조회보다 늦으면 없는 역이 404로 먼저 걸려 400이 가려진다
        mockMvc.perform(get("/api/v1/stations/{stationId}/places", 999L)
                        .param("travelStyles", "NOT_A_TAG"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"));
        verify(stationQueryService, never()).getStationPlaces(any(), any());
    }
}
