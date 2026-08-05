package com.cotato.nextstation.domain.place.controller;

import com.cotato.nextstation.domain.place.dto.response.PlaceReviewLikeResponse;
import com.cotato.nextstation.domain.place.exception.PlaceReviewErrorCode;
import com.cotato.nextstation.domain.place.service.command.PlaceReviewLikeCommandService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.GlobalExceptionHandler;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import com.cotato.nextstation.global.jwt.JwtProvider;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaceReviewLikeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PlaceReviewLikeControllerTest {

    private static final String TOKEN = "access-token";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PlaceReviewLikeCommandService placeReviewLikeCommandService;

    // WebConfig가 등록하는 JwtPrincipalArgumentResolver가 필요로 해서 @WebMvcTest 슬라이스에도 목이 필요하다
    @MockitoBean
    JwtProvider jwtProvider;

    @BeforeEach
    void authenticateAsMember1() {
        // 리졸버가 토큰에서 memberId를 꺼내므로, 토큰을 실은 요청은 1번 회원으로 인증된 것처럼 둔다
        given(jwtProvider.parseClaims(TOKEN)).willReturn(
                Jwts.claims().subject("1").add("purpose", "ACCESS").build());
    }

    @Test
    @DisplayName("좋아요는 토큰이 없으면 401이다")
    void likeReview_withoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/places/reviews/{reviewId}/like", 501L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("좋아요를 누르면 200과 좋아요 결과를 반환한다")
    void likeReview_success() throws Exception {
        given(placeReviewLikeCommandService.like(eq(1L), eq(501L)))
                .willReturn(new PlaceReviewLikeResponse(501L, 13L, true));

        mockMvc.perform(post("/api/v1/places/reviews/{reviewId}/like", 501L)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewId").value(501))
                .andExpect(jsonPath("$.data.likeCount").value(13))
                .andExpect(jsonPath("$.data.isLiked").value(true));
    }

    @Test
    @DisplayName("이미 좋아요를 누른 리뷰면 409를 반환한다")
    void likeReview_alreadyExists() throws Exception {
        given(placeReviewLikeCommandService.like(eq(1L), eq(501L)))
                .willThrow(new CustomException(PlaceReviewErrorCode.PLACE_REVIEW_LIKE_ALREADY_EXISTS));

        mockMvc.perform(post("/api/v1/places/reviews/{reviewId}/like", 501L)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_409_PLACE_REVIEW_LIKE_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("존재하지 않는 리뷰에 좋아요를 누르면 404를 반환한다")
    void likeReview_reviewNotFound() throws Exception {
        given(placeReviewLikeCommandService.like(eq(1L), eq(999L)))
                .willThrow(new CustomException(PlaceReviewErrorCode.PLACE_REVIEW_NOT_FOUND));

        mockMvc.perform(post("/api/v1/places/reviews/{reviewId}/like", 999L)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_PLACE_REVIEW_NOT_FOUND"));
    }

    @Test
    @DisplayName("좋아요 취소는 토큰이 없으면 401이다")
    void unlikeReview_withoutToken() throws Exception {
        mockMvc.perform(delete("/api/v1/places/reviews/{reviewId}/like", 501L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(GlobalErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("좋아요를 취소하면 200과 취소 결과를 반환한다")
    void unlikeReview_success() throws Exception {
        given(placeReviewLikeCommandService.unlike(eq(1L), eq(501L)))
                .willReturn(new PlaceReviewLikeResponse(501L, 12L, false));

        mockMvc.perform(delete("/api/v1/places/reviews/{reviewId}/like", 501L)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewId").value(501))
                .andExpect(jsonPath("$.data.likeCount").value(12))
                .andExpect(jsonPath("$.data.isLiked").value(false));
    }

    @Test
    @DisplayName("좋아요를 누르지 않은 리뷰를 취소하면 404를 반환한다")
    void unlikeReview_likeNotFound() throws Exception {
        given(placeReviewLikeCommandService.unlike(eq(1L), eq(501L)))
                .willThrow(new CustomException(PlaceReviewErrorCode.PLACE_REVIEW_LIKE_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/places/reviews/{reviewId}/like", 501L)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_ERROR_404_PLACE_REVIEW_LIKE_NOT_FOUND"));
    }

}
