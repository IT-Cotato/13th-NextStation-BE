package com.cotato.nextstation.domain.auth.controller;

import com.cotato.nextstation.domain.auth.dto.response.TermsResponse;
import com.cotato.nextstation.domain.auth.dto.response.TermsSummaryResponse;
import com.cotato.nextstation.domain.auth.entity.TermsType;
import com.cotato.nextstation.domain.auth.service.query.TermsQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/terms")
public class TermsController {

    private final TermsQueryService termsQueryService;

    @Tag(name = "약관")
    @Operation(
            summary = "약관 목록 조회",
            description = """
                    현재 노출 중인 약관 목록을 조회한다. 회원가입 동의 화면용.
                    - 같은 제목(title)의 약관 중 가장 최근에 추가된 버전만 반환한다.
                    - 필수 약관(isRequired=true)이 먼저, 그 다음 선택 약관 순으로 정렬된다.
                    - 원문(content)은 응답에 없다. 원문이 필요하면 `type`으로 단건 조회 API를 호출한다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
    })
    @GetMapping
    public CommonResponse<List<TermsSummaryResponse>> getTerms() {
        return CommonResponse.success(termsQueryService.getLatestTerms());
    }

    @Tag(name = "약관")
    @Operation(
            summary = "약관 단건 조회",
            description = """
                    약관 하나를 원문(content)까지 조회한다. 설정 > 이용약관/개인정보처리방침처럼 한 건만 보여주는 화면용.
                    - `type`은 `SERVICE`(서비스 이용약관) / `PRIVACY`(개인정보 수집 및 이용 동의) / `MARKETING`(마케팅 정보 수신 동의).
                    - 해당 종류의 최신 버전을 반환한다.
                    - 로그인 없이 호출할 수 있다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "정의되지 않은 약관 종류 (`GlobalErrorCode.VALIDATION_ERROR`)"),
            @ApiResponse(responseCode = "404", description = "아직 등록되지 않은 약관 (`AuthErrorCode.TERMS_NOT_FOUND`)"),
    })
    @GetMapping("/{type}")
    public CommonResponse<TermsResponse> getTerms(
            @Parameter(description = "약관 종류", example = "SERVICE") @PathVariable TermsType type) {
        return CommonResponse.success(termsQueryService.getLatestTerms(type));
    }
}