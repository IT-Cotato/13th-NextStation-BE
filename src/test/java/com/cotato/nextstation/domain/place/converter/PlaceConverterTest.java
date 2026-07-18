package com.cotato.nextstation.domain.place.converter;

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
        PlaceDetailResponse response = placeConverter.toDetailResponse(place, List.of(), List.of(), List.of());

        // then
        assertThat(response.images()).containsExactly("https://default.jpg");
    }
}