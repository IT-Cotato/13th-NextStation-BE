package com.cotato.nextstation.domain.journal.controller;

import com.cotato.nextstation.domain.journal.dto.request.JournalCreateRequest;
import com.cotato.nextstation.domain.journal.dto.request.JournalUpdateRequest;
import com.cotato.nextstation.domain.journal.dto.response.JournalCreateResponse;
import com.cotato.nextstation.domain.journal.dto.response.JournalDetailResponse;
import com.cotato.nextstation.domain.journal.dto.response.JournalWriteInfoResponse;
import com.cotato.nextstation.domain.journal.dto.response.UncompletedJournalListResponse;
import com.cotato.nextstation.domain.journal.service.command.JournalCommandService;
import com.cotato.nextstation.domain.journal.service.query.JournalQueryService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/journals")
public class JournalController {



    private final JournalCommandService journalCommandService;
    private final JournalQueryService journalQueryService;

    @Operation(summary = "여행일지 작성 초기 정보 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 스탬프"),
    })
    @SecurityRequirement(name = "accessTokenAuth")
    @GetMapping("/write-info")
    public CommonResponse<JournalWriteInfoResponse> getWriteInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam Long memberStampId) {
        return CommonResponse.success(journalQueryService.getWriteInfo(principal.memberId(), memberStampId));
    }

    @Operation(summary = "여행일지 작성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "작성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 스탬프 또는 장소"),
    })
    @SecurityRequirement(name = "accessTokenAuth")
    @PostMapping
    public CommonResponse<JournalCreateResponse> createJournal(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody JournalCreateRequest request) {
            Long journalId = journalCommandService.createJournal(principal.memberId(), request);

        return CommonResponse.success(new JournalCreateResponse(journalId));
    }

    @Operation(summary = "여행일지 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "본인 일지가 아님"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 여행일지"),
    })
    @SecurityRequirement(name = "accessTokenAuth")
    @PatchMapping("/{journalId}")
    public CommonResponse<Void> updateJournal(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long journalId,
            @Valid @RequestBody JournalUpdateRequest request) {
        journalCommandService.updateJournal(principal.memberId(), journalId, request);
        return CommonResponse.success(null);
    }

    @Operation(summary = "여행일지 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 여행일지"),
    })
    @SecurityRequirement(name = "accessTokenAuth")
    @GetMapping("/{journalId}")
    public CommonResponse<JournalDetailResponse> getJournalDetail(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long journalId) {
        Long memberId = principal != null ? principal.memberId() : null;
        return CommonResponse.success(journalQueryService.getJournalDetail(memberId, journalId));
    }

    @Operation(summary = "여행일지 미작성 목록 조회",
            description = "스탬프는 있는데 여행일지를 작성하지 않은 코스 목록을 최신순으로 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
    })
    @SecurityRequirement(name = "accessTokenAuth")
    @GetMapping("/uncompleted")
    public CommonResponse<UncompletedJournalListResponse> getUncompletedJournals(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal
    ) {
        return CommonResponse.success(journalQueryService.getUncompletedJournals(principal.memberId()));
    }

    @Operation(summary = "여행일지 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "본인 일지가 아님"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 여행일지"),
    })
    @SecurityRequirement(name = "accessTokenAuth")
    @DeleteMapping("/{journalId}")
    public CommonResponse<Void> deleteJournal(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long journalId
    ) {
        journalCommandService.deleteJournal(principal.memberId(), journalId);
        return CommonResponse.success(null);
    }
}