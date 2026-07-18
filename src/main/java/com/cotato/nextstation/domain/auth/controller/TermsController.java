package com.cotato.nextstation.domain.auth.controller;

import com.cotato.nextstation.domain.auth.dto.response.TermsResponse;
import com.cotato.nextstation.domain.auth.service.query.TermsQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/terms")
public class TermsController {

    private final TermsQueryService termsQueryService;

    @Operation(
            summary = "이용 약관 목록 조회",
            description = """
                    현재 노출 중인 이용 약관 목록을 조회한다.
                    - 같은 제목(title)의 약관 중 가장 최근에 추가된 버전만 반환한다.
                    - 필수 약관(isRequired=true)이 먼저, 그 다음 선택 약관 순으로 정렬된다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
    })
    @GetMapping
    public CommonResponse<List<TermsResponse>> getTerms() {
        return CommonResponse.success(termsQueryService.getLatestTerms());
    }
}
