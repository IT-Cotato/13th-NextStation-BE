package com.cotato.nextstation.domain.place.controller;

import com.cotato.nextstation.domain.place.dto.response.PlaceDetailResponse;
import com.cotato.nextstation.domain.place.service.query.PlaceQueryService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/places")
public class PlaceController {

    private final PlaceQueryService placeQueryService;

    @Operation(
            summary = "장소 상세 조회",
            description = """
                    장소 기본 정보, 이미지, 공개 리뷰 미리보기(최신순 최대 3개)를 함께 조회한다.
                    - 등록된 이미지가 없으면 카테고리 기본 이미지 1장으로 대체된다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 장소 (`PlaceErrorCode.PLACE_NOT_FOUND`)"),
    })
    @GetMapping("/{placeId}")
    public CommonResponse<PlaceDetailResponse> getPlaceDetail(@PathVariable Long placeId) {
        return CommonResponse.success(placeQueryService.getPlaceDetail(placeId));
    }
}