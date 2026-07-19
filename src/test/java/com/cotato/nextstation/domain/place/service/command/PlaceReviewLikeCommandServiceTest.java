package com.cotato.nextstation.domain.place.service.command;

import com.cotato.nextstation.domain.place.converter.PlaceReviewLikeConverter;
import com.cotato.nextstation.domain.place.dto.response.PlaceReviewLikeResponse;
import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.place.exception.PlaceReviewErrorCode;
import com.cotato.nextstation.domain.place.repository.PlaceReviewLikeRepository;
import com.cotato.nextstation.domain.place.repository.PlaceReviewRepository;
import com.cotato.nextstation.global.exception.CustomException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaceReviewLikeCommandServiceTest {

    @InjectMocks
    private PlaceReviewLikeCommandService placeReviewLikeCommandService;

    @Mock
    private PlaceReviewRepository placeReviewRepository;
    @Mock
    private PlaceReviewLikeRepository placeReviewLikeRepository;
    @Mock
    private PlaceReviewLikeConverter placeReviewLikeConverter;
    @Mock
    private EntityManager entityManager;

    @Test
    @DisplayName("좋아요를 취소하면 PlaceReviewLike가 삭제되고 likeCount가 감소한다")
    void unlike_success() {
        // given
        Long memberId = 1L;
        Long reviewId = 501L;
        PlaceReview review = mock(PlaceReview.class);
        given(review.getLikeCount()).willReturn(12L);


        given(placeReviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(placeReviewLikeRepository.deleteByMemberIdAndPlaceReview(memberId, review)).willReturn(1);

        PlaceReviewLikeResponse expected = new PlaceReviewLikeResponse(reviewId, 12L, false);
        given(placeReviewLikeConverter.toLikeResponse(reviewId, 12L, false)).willReturn(expected);

        // when
        PlaceReviewLikeResponse result = placeReviewLikeCommandService.unlike(memberId, reviewId);

        // then
        assertThat(result).isEqualTo(expected);
        verify(placeReviewRepository).decrementLikeCount(reviewId);
        verify(entityManager).refresh(review);
    }

    @Test
    @DisplayName("좋아요를 누르지 않은 리뷰를 취소하려 하면 예외가 발생한다")
    void unlike_likeNotFound() {
        // given
        Long memberId = 1L;
        Long reviewId = 501L;
        PlaceReview review = mock(PlaceReview.class);

        given(placeReviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(placeReviewLikeRepository.deleteByMemberIdAndPlaceReview(memberId, review)).willReturn(0);

        // when & then
        assertThatThrownBy(() -> placeReviewLikeCommandService.unlike(memberId, reviewId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(PlaceReviewErrorCode.PLACE_REVIEW_LIKE_NOT_FOUND.getMessage());

        verify(placeReviewRepository, never()).decrementLikeCount(any());
    }
}