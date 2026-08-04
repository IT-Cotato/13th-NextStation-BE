package com.cotato.nextstation.domain.member.controller;

import com.cotato.nextstation.domain.course.dto.response.MemberCourseListResponse;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.domain.member.dto.response.MemberProfileResponse;
import com.cotato.nextstation.domain.member.dto.response.OtherMemberProfileResponse;
import com.cotato.nextstation.domain.member.service.query.MemberQueryService;
import com.cotato.nextstation.domain.stamp.dto.response.MemberStampListResponse;
import com.cotato.nextstation.domain.stamp.service.query.MemberStampQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import com.cotato.nextstation.global.security.AuthenticationPrincipal;
import com.cotato.nextstation.global.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberQueryService memberQueryService;
    private final MemberStampQueryService memberStampQueryService;
    private final CourseQueryService courseQueryService;

    @Operation(
            summary = "내 프로필 조회",
            description = """
                    로그인한 회원의 닉네임/프로필 이미지를 조회한다.
                    - accessToken 인증 필요. 우측 상단 자물쇠(Authorize) 버튼을 눌러 로그인 API 응답의 accessToken 값을(Bearer 접두사 없이) 넣으면 된다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원 (`MemberErrorCode.MEMBER_NOT_FOUND`)"),
    })
    @GetMapping("/me")
    public CommonResponse<MemberProfileResponse> getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal) {
        return CommonResponse.success(memberQueryService.getMyProfile(principal.memberId()));
    }

    @Operation(
            summary = "다른 회원 프로필 조회",
            description = """
                    다른 회원의 프로필(닉네임/프로필 이미지/스탬프 개수/공개 코스 개수)을 조회한다.
                    - accessToken 인증 필요. 우측 상단 자물쇠(Authorize) 버튼을 눌러 로그인 API 응답의 accessToken 값을(Bearer 접두사 없이) 넣으면 된다.
                    - 프로필 화면 상단 헤더용 정보다. 스탬프 탭 목록은 `GET /members/{memberId}/stamps`, 공개코스 탭 목록은 `GET /members/{memberId}/courses`로 별도 조회한다.
                    - 스탬프 개수는 방문한(스탬프를 찍은) 서로 다른 역의 개수다. 같은 역에서 여러 코스를 완료해도 1개로 센다.
                    - 공개 코스 개수는 그 회원이 만든 코스 중 여행일지가 공개된 코스만 센다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "memberId가 1 미만 (`GlobalErrorCode.VALIDATION_ERROR`)"),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원 (`MemberErrorCode.MEMBER_NOT_FOUND`)"),
    })
    @GetMapping("/{memberId}/profile")
    public CommonResponse<OtherMemberProfileResponse> getMemberProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "조회할 회원 ID", example = "2")
            @PathVariable @Positive Long memberId) {
        return CommonResponse.success(memberQueryService.getMemberProfile(memberId));
    }

    @Operation(
            summary = "다른 회원 스탬프 목록 조회",
            description = """
                    다른 회원이 모은 스탬프(방문한 역) 목록을 조회한다.
                    - 프로필 화면의 스탬프 탭에서 사용한다.
                    - 같은 역에서 여러 코스를 완료했어도 역당 스탬프 1개로 묶어서 보여준다.
                    - 내 스탬프 목록(`GET /stamps`)과 같은 정렬 기준을 쓴다: 1호선 → 9호선 순, 대표 호선이 없는 역은 맨 뒤.
                      동일 호선 내에서는 역명 가나다순으로 정렬된다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "memberId가 1 미만 (`GlobalErrorCode.VALIDATION_ERROR`)"),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원 (`MemberErrorCode.MEMBER_NOT_FOUND`)"),
    })
    @GetMapping("/{memberId}/stamps")
    public CommonResponse<MemberStampListResponse> getMemberStamps(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "조회할 회원 ID", example = "2")
            @PathVariable @Positive Long memberId) {
        return CommonResponse.success(memberStampQueryService.getMemberStamps(memberId));
    }

    @Operation(
            summary = "다른 회원 공개 코스 목록 조회",
            description = """
                    다른 회원이 만든 코스 중 여행일지가 공개된 코스만 최신순으로 조회한다.
                    - 프로필 화면의 공개코스 탭에서 사용한다.
                    - `nextCursor`를 그대로 `cursor`에 넣어 다음 페이지를 요청한다.
                    """
    )
    @SecurityRequirement(name = "accessTokenAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "memberId가 1 미만 (`GlobalErrorCode.VALIDATION_ERROR`), size 범위를 벗어남 (`GlobalErrorCode.INVALID_PAGE_SIZE`), 또는 커서가 잘못됨 (`GlobalErrorCode.INVALID_CURSOR`)"),
            @ApiResponse(responseCode = "401", description = "accessToken 누락, 위변조, 또는 만료 (`GlobalErrorCode.UNAUTHORIZED`, `GlobalErrorCode.INVALID_TOKEN`, `GlobalErrorCode.EXPIRED_TOKEN`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원 (`MemberErrorCode.MEMBER_NOT_FOUND`)"),
    })
    @GetMapping("/{memberId}/courses")
    public CommonResponse<MemberCourseListResponse> getMemberCourses(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "조회할 회원 ID", example = "2")
            @PathVariable @Positive Long memberId,
            @Parameter(description = "다음 페이지 커서 (첫 페이지는 생략)")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기 (1~50, 기본 10)", example = "10")
            @RequestParam(required = false) Integer size) {
        return CommonResponse.success(courseQueryService.getMemberPublicCourses(memberId, cursor, size));
    }
}
