package com.cotato.nextstation.domain.recommendation.controller;

import com.cotato.nextstation.domain.recommendation.dto.request.CustomRecommendationRequest;
import com.cotato.nextstation.domain.recommendation.dto.response.CustomRecommendationResponse;
import com.cotato.nextstation.domain.recommendation.service.command.RecommendationCommandService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import com.cotato.nextstation.global.security.AuthenticationPrincipal;
import com.cotato.nextstation.global.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
public class CustomRecommendationController {

    private final RecommendationCommandService recommendationCommandService;

    @Operation(
            summary = "맞춤추천",
            description = """
                    출발역·이동 가능 시간·여행 스타일(최소 1개, 최대 3개)을 받아 조건에 맞는 역 하나를 추천한다.
                    - 출발역에서 이동 가능 시간 내에 닿는 뽑기 대상 역만 후보로 둔다(상관없음이면 시간 제한 없음).
                    - 여행 스타일 태그로 역 점수(장소 수 합 + 충족 태그 수 × 10)를 매기되, 가본 역(스탬프 있는 역)은 4점 감점한 뒤 상위 5개 역을 후보군으로 묶는다.
                    - 직전 추천 역은 제외한다. 제외 후 후보가 없으면 이 조건도 적용하지 않는다.
                    - 남은 후보 중 무작위로 하나를 고른다.
                    - 결과 역에서 코스를 만들려면 역별 장소 목록 조회로 이어진다.

                    accessToken은 **선택**이다. 없으면 비로그인 추천으로 동작한다(가본 역 감점·직전 추천 제외 미적용).
                    보냈는데 만료·위조면 401이다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 — 여행 스타일이 1~3개가 아니거나, 존재하지 않거나, 중복됨 (`GlobalErrorCode.VALIDATION_ERROR`)"),
            @ApiResponse(responseCode = "401", description = "accessToken을 보냈으나 위변조 또는 만료 (`GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 출발역 또는 조건에 맞게 갈 수 있는 역이 없음 (`RecommendationErrorCode.DEPARTURE_STATION_NOT_FOUND`, `NO_REACHABLE_STATION`)"),
    })
    @PostMapping("/custom")
    public CommonResponse<CustomRecommendationResponse> recommendCustom(
            // 비로그인도 추천받을 수 있어야 해서 required = false다. 로그인 시에만 가본 역 감점·직전 추천 제외가 적용된다.
            @Parameter(hidden = true) @AuthenticationPrincipal(required = false) JwtPrincipal principal,
            @Valid @RequestBody CustomRecommendationRequest request) {
        Long memberId = (principal != null) ? principal.memberId() : null;
        return CommonResponse.success(recommendationCommandService.recommendCustom(memberId, request));
    }
}
