package com.cotato.nextstation.domain.departure.controller;

import com.cotato.nextstation.domain.departure.dto.request.DepartureStationCreateRequest;
import com.cotato.nextstation.domain.departure.dto.response.DepartureStationCreateResponse;
import com.cotato.nextstation.domain.departure.dto.response.DepartureStationResponse;
import com.cotato.nextstation.domain.departure.exception.DepartureStationErrorCode;
import com.cotato.nextstation.domain.departure.service.command.DepartureStationCommandService;
import com.cotato.nextstation.domain.departure.service.query.DepartureStationQueryService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
import com.cotato.nextstation.global.jwt.JwtProvider;
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
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DepartureStationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DepartureStationControllerTest {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    @Autowired
    MockMvc mockMvc;

    // @WebMvcTest 슬라이스에 ObjectMapper 빈이 노출되지 않아 요청 직렬화용으로 직접 생성한다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    DepartureStationCommandService departureStationCommandService;

    @MockitoBean
    DepartureStationQueryService departureStationQueryService;

    // WebConfig가 등록하는 JwtPrincipalArgumentResolver가 필요로 해서 @WebMvcTest 슬라이스에도 목이 필요하다
    @MockitoBean
    JwtProvider jwtProvider;

    @Test
    @DisplayName("출발역 추가는 201과 저장된 데이터를 반환한다")
    void addDepartureStation_created() throws Exception {
        DepartureStationCreateRequest request = new DepartureStationCreateRequest(100L);
        given(departureStationCommandService.addDepartureStation(eq(1L), any()))
                .willReturn(new DepartureStationCreateResponse(1L, 100L, 1, LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/departure-stations")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stationId").value(100))
                .andExpect(jsonPath("$.data.orderNum").value(1));
    }

    @Test
    @DisplayName("출발역이 10개를 초과하면 409를 반환한다")
    void addDepartureStation_maxExceeded() throws Exception {
        DepartureStationCreateRequest request = new DepartureStationCreateRequest(100L);
        given(departureStationCommandService.addDepartureStation(eq(1L), any()))
                .willThrow(new CustomException(DepartureStationErrorCode.MAX_DEPARTURE_STATIONS_EXCEEDED));

        mockMvc.perform(post("/api/v1/departure-stations")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_409_MAX_DEPARTURE_STATIONS_EXCEEDED"));
    }

    @Test
    @DisplayName("이미 추가한 역이면 409를 반환한다")
    void addDepartureStation_duplicate() throws Exception {
        DepartureStationCreateRequest request = new DepartureStationCreateRequest(100L);
        given(departureStationCommandService.addDepartureStation(eq(1L), any()))
                .willThrow(new CustomException(DepartureStationErrorCode.DUPLICATE_DEPARTURE_STATION));

        mockMvc.perform(post("/api/v1/departure-stations")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_409_DUPLICATE_DEPARTURE_STATION"));
    }

    @Test
    @DisplayName("역 ID가 없으면 검증 오류로 400을 반환한다")
    void addDepartureStation_validation() throws Exception {
        DepartureStationCreateRequest request = new DepartureStationCreateRequest(null);

        mockMvc.perform(post("/api/v1/departure-stations")
                        .header(MEMBER_ID_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_400_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.reasons.stationId").exists());
    }

    @Test
    @DisplayName("출발역 목록을 조회한다")
    void getDepartureStations_success() throws Exception {
        given(departureStationQueryService.getDepartureStations(1L))
                .willReturn(List.of(
                        new DepartureStationResponse(1L, 100L, "왕십리역", List.of("2호선"), 1, LocalDateTime.now()),
                        new DepartureStationResponse(2L, 200L, "강남역", List.of("2호선", "신분당선"), 2, LocalDateTime.now())));

        mockMvc.perform(get("/api/v1/departure-stations")
                        .header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].stationName").value("왕십리역"))
                .andExpect(jsonPath("$.data[1].lines[1]").value("신분당선"))
                .andExpect(jsonPath("$.data[0].orderNum").value(1));
    }

    @Test
    @DisplayName("출발역을 삭제하면 200을 반환한다")
    void deleteDepartureStation_success() throws Exception {
        doNothing().when(departureStationCommandService).deleteDepartureStation(1L, 5L);

        mockMvc.perform(delete("/api/v1/departure-stations/{id}", 5L)
                        .header(MEMBER_ID_HEADER, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
