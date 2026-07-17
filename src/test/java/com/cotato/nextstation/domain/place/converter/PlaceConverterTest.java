package com.cotato.nextstation.domain.place.converter;

import com.cotato.nextstation.domain.journal.entity.Journal;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.place.dto.response.PlaceDetailResponse;
import com.cotato.nextstation.domain.place.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.given;

class PlaceConverterTest {

    private final PlaceConverter placeConverter = new PlaceConverter();

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
        PlaceDetailResponse response = placeConverter.toDetailResponse(place, List.of(), List.of());

        // then
        assertThat(response.images()).containsExactly("https://default.jpg");
    }

    @Test
    @DisplayName("리뷰가 4개 이상이어도 최신순 3개까지만 반환한다")
    void toDetailResponse_limitReviewsToThree() {
        // given
        Category category = mock(Category.class);
        given(category.getName()).willReturn("식당");

        Place place = mock(Place.class);
        given(place.getCategory()).willReturn(category);

        List<PlaceReview> reviews = List.of(
                mockReview("리뷰1"),
                mockReview("리뷰2"),
                mockReview("리뷰3"),
                mockReview("리뷰4")
        );

        // when
        PlaceDetailResponse response = placeConverter.toDetailResponse(place, List.of(), reviews);

        // then
        assertThat(response.reviews()).hasSize(3);
    }

    private PlaceReview mockReview(String content) {
        Member member = mock(Member.class);
        given(member.getNickname()).willReturn("테스트유저");

        Journal journal = mock(Journal.class);
        given(journal.getMember()).willReturn(member);

        PlaceReview review = mock(PlaceReview.class);
        given(review.getJournal()).willReturn(journal);
        given(review.getReview()).willReturn(content);

        return review;
    }
}