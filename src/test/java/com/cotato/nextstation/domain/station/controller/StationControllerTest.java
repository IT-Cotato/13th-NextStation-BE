package com.cotato.nextstation.domain.station.controller;

import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.service.query.StationQueryService;
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

    // WebConfig가 등록하는 JwtPrincipalArgumentResolver가 필요로 해서 @WebMvcTest 슬라이스에도 목이 필요하다
    @MockitoBean
    JwtProvider jwtProvider;

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
}
