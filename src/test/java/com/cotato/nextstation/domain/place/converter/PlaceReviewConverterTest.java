package com.cotato.nextstation.domain.place.converter;

import com.cotato.nextstation.domain.journal.entity.Journal;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.place.dto.response.PlaceReviewListResponse;
import com.cotato.nextstation.domain.place.dto.response.PlaceReviewResponse;
import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.place.entity.PlaceReviewImage;
import com.cotato.nextstation.domain.place.repository.PlaceReviewImageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PlaceReviewConverterTest {

    @InjectMocks
    private PlaceReviewConverter placeReviewConverter;

    @Mock
    private PlaceReviewImageRepository placeReviewImageRepository;

    private PlaceReview mockReview(Long id, String content, Long writerId, String nickname) {
        Member member = mock(Member.class);
        lenient().when(member.getId()).thenReturn(writerId);
        lenient().when(member.getNickname()).thenReturn(nickname);
        lenient().when(member.getProfileImageUrl()).thenReturn("https://s3.../profile/" + writerId + ".jpg");

        Journal journal = mock(Journal.class);
        lenient().when(journal.getMember()).thenReturn(member);

        PlaceReview review = mock(PlaceReview.class);
        lenient().when(review.getId()).thenReturn(id);
        lenient().when(review.getJournal()).thenReturn(journal);
        lenient().when(review.getReview()).thenReturn(content);
        lenient().when(review.getLikeCount()).thenReturn(10L);
        lenient().when(review.getCreatedAt()).thenReturn(LocalDateTime.now());

        return review;
    }

    @Test
    @DisplayName("리뷰 id별로 이미지를 그룹핑한다")
    void resolveImagesByReviewId_success() {
        // given
        PlaceReview review1 = mockReview(501L, "리뷰1", 1L, "닉네임1");
        PlaceReview review2 = mockReview(502L, "리뷰2", 2L, "닉네임2");

        PlaceReviewImage image1 = mock(PlaceReviewImage.class);
        given(image1.getPlaceReview()).willReturn(review1);
        given(image1.getImageUrl()).willReturn("https://img1.jpg");

        PlaceReviewImage image2 = mock(PlaceReviewImage.class);
        given(image2.getPlaceReview()).willReturn(review1);
        given(image2.getImageUrl()).willReturn("https://img2.jpg");

        given(placeReviewImageRepository.findByPlaceReviewIdIn(List.of(501L, 502L)))
                .willReturn(List.of(image1, image2));

        // when
        Map<Long, List<String>> result = placeReviewConverter.resolveImagesByReviewId(List.of(review1, review2));

        // then
        assertThat(result.get(501L)).containsExactly("https://img1.jpg", "https://img2.jpg");
        assertThat(result.containsKey(502L)).isFalse();
    }

    @Test
    @DisplayName("리뷰 목록이 비어있으면 빈 맵을 반환한다")
    void resolveImagesByReviewId_emptyReviews() {
        // when
        Map<Long, List<String>> result = placeReviewConverter.resolveImagesByReviewId(List.of());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("좋아요 누른 리뷰는 isLiked가 true로 매핑된다")
    void toListResponse_isLiked_mapping() {
        // given
        PlaceReview review1 = mockReview(501L, "리뷰1", 1L, "닉네임1");
        PlaceReview review2 = mockReview(502L, "리뷰2", 2L, "닉네임2");

        // when
        PlaceReviewListResponse response = placeReviewConverter.toListResponse(
                24L, List.of(review1, review2), Map.of(), Set.of(501L), null, false);

        // then
        PlaceReviewResponse response1 = response.reviews().stream().filter(r -> r.reviewId().equals(501L)).findFirst().orElseThrow();
        PlaceReviewResponse response2 = response.reviews().stream().filter(r -> r.reviewId().equals(502L)).findFirst().orElseThrow();

        assertThat(response1.isLiked()).isTrue();
        assertThat(response2.isLiked()).isFalse();
    }

    @Test
    @DisplayName("totalCount는 전달받은 값을 그대로 응답에 담는다 (null 포함)")
    void toListResponse_totalCount_passThrough() {
        // given
        PlaceReview review1 = mockReview(501L, "리뷰1", 1L, "닉네임1");

        // when
        PlaceReviewListResponse withCount = placeReviewConverter.toListResponse(
                24L, List.of(review1), Map.of(), Set.of(), null, false);
        PlaceReviewListResponse withoutCount = placeReviewConverter.toListResponse(
                null, List.of(review1), Map.of(), Set.of(), null, false);

        // then
        assertThat(withCount.totalCount()).isEqualTo(24L);
        assertThat(withoutCount.totalCount()).isNull();
    }


    @Test
    @DisplayName("리뷰 이미지는 1개만 반환된다 (기획상 이미지 1개 제한)")
    void toListResponse_singleImageUrl() {
        // given
        PlaceReview review = mockReview(501L, "리뷰", 1L, "닉네임");

        // imageUrl이 있는 경우
        Map<Long, List<String>> imagesWithUrl = Map.of(501L, List.of("https://img1.jpg", "https://img2.jpg"));

        // when
        PlaceReviewListResponse response = placeReviewConverter.toListResponse(
                1L, List.of(review), imagesWithUrl, Set.of(), null, false);

        // then — 첫 번째 이미지 1개만 반환
        PlaceReviewResponse result = response.reviews().get(0);
        assertThat(result.imageUrl()).isEqualTo("https://img1.jpg");
    }


    @Test
    @DisplayName("리뷰 이미지가 없으면 imageUrl이 null이다")
    void toListResponse_nullImageUrl() {
        // given
        PlaceReview review = mockReview(501L, "리뷰", 1L, "닉네임");

        // when
        PlaceReviewListResponse response = placeReviewConverter.toListResponse(
                1L, List.of(review), Map.of(), Set.of(), null, false);

        // then
        PlaceReviewResponse result = response.reviews().get(0);
        assertThat(result.imageUrl()).isNull();
    }
}


