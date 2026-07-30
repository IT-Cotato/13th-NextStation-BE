package com.cotato.nextstation.domain.recommendation.controller;

import com.cotato.nextstation.domain.recommendation.dto.response.RandomRecommendationResponse;
import com.cotato.nextstation.domain.recommendation.service.command.RecommendationCommandService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import com.cotato.nextstation.global.security.AuthenticationPrincipal;
import com.cotato.nextstation.global.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/random")
public class RandomController {

    private final RecommendationCommandService recommendationCommandService;

    @Operation(
            summary = "랜덤뽑기",
            description = """
                    뽑기 대상 역 중 무작위로 1개를 뽑아 역 정보와 코스 미리보기를 반환한다.
                    - 로그인 시 직전 추천 1건을 제외한다. 제외 후 후보가 없으면 전체에서 다시 뽑는다.
                    - 환승역이면 소속 노선을 모두 반환하며, 대표 노선이 목록 맨 앞에 온다.
                    - 코스 미리보기는 카테고리별 장소 1개씩으로 구성되며, 장소가 없는 카테고리는 제외된다.
                    - 코스 미리보기는 저장되지 않는다. 저장은 코스 저장 API에서만 일어난다.

                    accessToken은 **선택**이다. 없으면 비로그인 뽑기로 동작하고,
                    보냈는데 만료·위조면 401이다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "뽑기 성공"),
            @ApiResponse(responseCode = "401", description = "accessToken을 보냈으나 위변조 또는 만료 (`GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "404", description = "뽑기 대상 역이 없음"),
    })
    @PostMapping
    public CommonResponse<RandomRecommendationResponse> drawRandom(
            // 비로그인도 뽑을 수 있어야 해서 required = false다. 로그인 시에만 직전 추천을 제외한다.
            @Parameter(hidden = true) @AuthenticationPrincipal(required = false) JwtPrincipal principal) {
        Long memberId = (principal != null) ? principal.memberId() : null;
        return CommonResponse.success(recommendationCommandService.drawRandom(memberId));
    }
}
