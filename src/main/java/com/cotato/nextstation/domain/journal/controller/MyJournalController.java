package com.cotato.nextstation.domain.journal.controller;

import com.cotato.nextstation.domain.journal.dto.response.MyJournalListResponse;
import com.cotato.nextstation.domain.journal.service.query.JournalQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import com.cotato.nextstation.global.security.AuthenticationPrincipal;
import com.cotato.nextstation.global.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// JournalController와 같은 태그를 써서 Swagger에서 한 섹션으로 보이게 한다.
@Tag(name = "Journal")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members/me/journals")
public class MyJournalController {

    private final JournalQueryService journalQueryService;

    @Operation(summary = "내 여행일지 목록 조회", description = "본인이 작성한 여행일지를 최신순으로 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료"),
    })
    @SecurityRequirement(name = "accessTokenAuth")
    @GetMapping
    public CommonResponse<MyJournalListResponse> getMyJournals(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal) {
        return CommonResponse.success(journalQueryService.getMyJournals(principal.memberId()));
    }
}
