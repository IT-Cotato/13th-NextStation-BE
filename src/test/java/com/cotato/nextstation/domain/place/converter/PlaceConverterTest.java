package com.cotato.nextstation.domain.place.converter;

import com.cotato.nextstation.domain.journal.entity.Journal;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.place.dto.response.PlaceDetailResponse;
import com.cotato.nextstation.domain.place.entity.*;
import com.cotato.nextstation.domain.place.repository.PlaceImageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PlaceConverterTest {


    @InjectMocks
    private PlaceConverter placeConverter;

    @Mock
    private PlaceImageRepository placeImageRepository;

    @Test
    @DisplayName("등록된 이미지가 없으면 카테고리 기본 이미지로 대체된다")
    void toDetailResponse_fallbackToDefaultImage() {
        // given
        Category category = mock(Category.class);
        given(category.getDefaultImageUrl()).willReturn("https://default.jpg");
        given(category.getName()).willReturn("식당");

        Place place = mock(Place.class);
        given(place.getCategory()).willReturn(category);

        // when
        PlaceDetailResponse response = placeConverter.toDetailResponse(place, 0L, List.of(), List.of(), List.of());

        // then
        assertThat(response.images()).containsExactly("https://default.jpg");
    }

    @Test
    @DisplayName("totalReviewCount가 응답에 정확히 포함된다")
    void toDetailResponse_totalReviewCount() {
        // given
        Category category = mock(Category.class);
        given(category.getDefaultImageUrl()).willReturn(null);
        given(category.getName()).willReturn("카페");

        Place place = mock(Place.class);
        given(place.getCategory()).willReturn(category);

        // when
        PlaceDetailResponse response = placeConverter.toDetailResponse(
                place, 24L, List.of(), List.of(), List.of()
        );

        // then
        assertThat(response.totalReviewCount()).isEqualTo(24L);
    }


    @Test
    @DisplayName("리뷰 이미지가 여러 개면 첫 번째 이미지만 응답에 포함된다")
    void toDetailResponse_reviewPreview_multipleImages_picksFirst() {
        // given
        Category category = mock(Category.class);
        given(category.getDefaultImageUrl()).willReturn(null);
        given(category.getName()).willReturn("카페");

        Place place = mock(Place.class);
        given(place.getCategory()).willReturn(category);

        Member member = mock(Member.class);
        given(member.getId()).willReturn(1L);
        given(member.getNickname()).willReturn("닉네임");
        given(member.getProfileImageUrl()).willReturn("http://profile.jpg");

        Journal journal = mock(Journal.class);
        given(journal.getMember()).willReturn(member);

        PlaceReview review = mock(PlaceReview.class);
        given(review.getId()).willReturn(10L);
        given(review.getJournal()).willReturn(journal);
        given(review.getReview()).willReturn("맛있어요");

        PlaceReviewImage firstImage = mock(PlaceReviewImage.class);
        given(firstImage.getPlaceReview()).willReturn(review);
        given(firstImage.getImageUrl()).willReturn("http://first.jpg");

        PlaceReviewImage secondImage = mock(PlaceReviewImage.class);
        given(secondImage.getPlaceReview()).willReturn(review);
        given(secondImage.getImageUrl()).willReturn("http://second.jpg");

        // when
        PlaceDetailResponse response = placeConverter.toDetailResponse(
                place, 1L, List.of(), List.of(review), List.of(firstImage, secondImage)
        );

        // then
        assertThat(response.reviews()).hasSize(1);
        assertThat(response.reviews().get(0).imageUrl()).isEqualTo("http://first.jpg");
    }

    @Test
    @DisplayName("리뷰 이미지가 없으면 imageUrl은 null이다")
    void toDetailResponse_reviewPreview_noImages_returnsNull() {
        // given
        Category category = mock(Category.class);
        given(category.getDefaultImageUrl()).willReturn(null);
        given(category.getName()).willReturn("카페");

        Place place = mock(Place.class);
        given(place.getCategory()).willReturn(category);

        Member member = mock(Member.class);
        given(member.getId()).willReturn(1L);
        given(member.getNickname()).willReturn("닉네임");
        given(member.getProfileImageUrl()).willReturn("http://profile.jpg");

        Journal journal = mock(Journal.class);
        given(journal.getMember()).willReturn(member);

        PlaceReview review = mock(PlaceReview.class);
        given(review.getId()).willReturn(10L);
        given(review.getJournal()).willReturn(journal);
        given(review.getReview()).willReturn("맛있어요");

        // when
        PlaceDetailResponse response = placeConverter.toDetailResponse(
                place, 1L, List.of(), List.of(review), List.of()
        );

        // then
        assertThat(response.reviews()).hasSize(1);
        assertThat(response.reviews().get(0).imageUrl()).isNull();
    }
}