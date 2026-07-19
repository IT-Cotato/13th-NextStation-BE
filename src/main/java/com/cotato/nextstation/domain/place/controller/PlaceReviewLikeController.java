package com.cotato.nextstation.domain.place.controller;

import com.cotato.nextstation.domain.place.dto.response.PlaceReviewLikeResponse;
import com.cotato.nextstation.domain.place.service.command.PlaceReviewLikeCommandService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/places/reviews")
public class PlaceReviewLikeController {

    // TODO: Auth 적용 시 X-Member-Id 헤더를 @AuthenticationPrincipal 로 교체한다.
    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String MEMBER_ID_DESCRIPTION = "회원 ID (Auth 적용 전까지 사용하는 임시 헤더)";

    private final PlaceReviewLikeCommandService placeReviewLikeCommandService;

    @Operation(
            summary = "장소 리뷰 좋아요",
            description = """
                장소 리뷰에 좋아요를 추가한다.
                - 현재 isLiked 상태에 따라
                  이 API(좋아요 추가)와 좋아요 취소 API(DELETE)를 구분해서 호출해야 한다.
                - 이미 좋아요를 누른 리뷰에 다시 호출하면 실패한다.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좋아요 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리뷰 (`PlaceReviewErrorCode.PLACE_REVIEW_NOT_FOUND`)"),
            @ApiResponse(responseCode = "409", description = "이미 좋아요를 누른 리뷰 (`PlaceReviewErrorCode.PLACE_REVIEW_LIKE_ALREADY_EXISTS`)"),
    })
    @PostMapping("/{reviewId}/like")
    public CommonResponse<PlaceReviewLikeResponse> likeReview(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Parameter(description = "리뷰 ID", example = "501")
            @PathVariable Long reviewId) {
        return CommonResponse.success(placeReviewLikeCommandService.like(memberId, reviewId));
    }

    @Operation(
            summary = "장소 리뷰 좋아요 취소",
            description = "장소 리뷰에 남긴 좋아요를 취소한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좋아요 취소 성공"),
            @ApiResponse(responseCode = "404", description = """
                    존재하지 않는 리뷰 (`PlaceReviewErrorCode.PLACE_REVIEW_NOT_FOUND`)
                    또는 좋아요를 누르지 않은 리뷰 (`PlaceReviewErrorCode.PLACE_REVIEW_LIKE_NOT_FOUND`)"""),
    })
    @DeleteMapping("/{reviewId}/like")
    public CommonResponse<PlaceReviewLikeResponse> unlikeReview(
            @Parameter(description = MEMBER_ID_DESCRIPTION, example = "1")
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Parameter(description = "리뷰 ID", example = "501")
            @PathVariable Long reviewId) {
        return CommonResponse.success(placeReviewLikeCommandService.unlike(memberId, reviewId));
    }
}