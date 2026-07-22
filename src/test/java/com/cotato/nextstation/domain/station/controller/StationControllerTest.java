package com.cotato.nextstation.domain.station.controller;

import com.cotato.nextstation.domain.station.dto.response.StationPlaceCategoryResponse;
import com.cotato.nextstation.domain.station.dto.response.StationPlaceResponse;
import com.cotato.nextstation.domain.station.dto.response.StationPlacesResponse;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.exception.StationErrorCode;
import com.cotato.nextstation.domain.station.service.query.StationQueryService;
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

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    @DisplayName("역 검색은 200과 역/노선 목록을 반환한다")
    void searchStations_success() throws Exception {
        given(stationQueryService.searchByName("왕십리역")).willReturn(List.of(
                new StationSummaryResponse(42L, "왕십리역", List.of("2호선", "5호선"))));

        mockMvc.perform(get("/api/v1/stations").param("keyword", "왕십리역"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stationId").value(42))
                .andExpect(jsonPath("$.data[0].stationName").value("왕십리역"))
                .andExpect(jsonPath("$.data[0].lines[0]").value("2호선"))
                .andExpect(jsonPath("$.data[0].lines[1]").value("5호선"));
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

    @Test
    @DisplayName("역별 장소 목록은 200과 카테고리별 장소를 반환한다")
    void getStationPlaces_success() throws Exception {
        given(stationQueryService.getStationPlaces(6L)).willReturn(
                new StationPlacesResponse(6L, "보문역", "성북천을 따라 걷기 좋은 역", "6호선",
                        List.of("6호선", "우이신설선"), List.of("LOCAL_EXPLORE", "NATURE"),
                        "보문역 환승여행 코스", List.of(
                        new StationPlaceCategoryResponse("CULTURE", "문화공간", List.of(
                                new StationPlaceResponse(11L, "보문숲길도서관", "동네 도서관", "img", 127.0, 37.5)))
                )));

        mockMvc.perform(get("/api/v1/stations/{stationId}/places", 6L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stationName").value("보문역"))
                .andExpect(jsonPath("$.data.description").value("성북천을 따라 걷기 좋은 역"))
                .andExpect(jsonPath("$.data.lineName").value("6호선"))
                .andExpect(jsonPath("$.data.lines[1]").value("우이신설선"))
                .andExpect(jsonPath("$.data.tags[0]").value("LOCAL_EXPLORE"))
                .andExpect(jsonPath("$.data.defaultCourseName").value("보문역 환승여행 코스"))
                .andExpect(jsonPath("$.data.categories[0].categoryCode").value("CULTURE"))
                .andExpect(jsonPath("$.data.categories[0].places[0].placeName").value("보문숲길도서관"));
    }

    @Test
    @DisplayName("장소가 없는 역은 200과 빈 카테고리 목록을 반환한다")
    void getStationPlaces_noPlaces() throws Exception {
        given(stationQueryService.getStationPlaces(300L))
                .willReturn(new StationPlacesResponse(300L, "서울역", null, null,
                        List.of("1호선", "4호선"), List.of(), "서울역 환승여행 코스", List.of()));

        mockMvc.perform(get("/api/v1/stations/{stationId}/places", 300L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories").isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 역이면 404를 반환한다")
    void getStationPlaces_notFound() throws Exception {
        given(stationQueryService.getStationPlaces(999L))
                .willThrow(new CustomException(StationErrorCode.STATION_NOT_FOUND));

        mockMvc.perform(get("/api/v1/stations/{stationId}/places", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_STATION_NOT_FOUND"));
    }
}
