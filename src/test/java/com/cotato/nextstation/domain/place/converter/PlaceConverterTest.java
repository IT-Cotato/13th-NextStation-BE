package com.cotato.nextstation.domain.place.converter;

import com.cotato.nextstation.domain.place.dto.response.PlaceDetailResponse;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.entity.*;
import com.cotato.nextstation.domain.place.enums.CategoryCode;
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

    @Test
    @DisplayName("Place 목록을 PlaceInfoResponse 목록으로 변환한다")
    void toPlaceInfoResponses_success() {
        // given
        Category category = mock(Category.class);
        given(category.getCode()).willReturn(CategoryCode.CULTURE);

        Place place = mock(Place.class);
        given(place.getId()).willReturn(1L);
        given(place.getPlaceName()).willReturn("보문숲길도서관");
        given(place.getDescription()).willReturn("혼자 조용히 머물기 좋은 동네 도서관");
        given(place.getCategory()).willReturn(category);
        given(place.getXCoordinate()).willReturn(127.123);
        given(place.getYCoordinate()).willReturn(37.456);

        // when
        List<PlaceInfoResponse> result = placeConverter.toPlaceInfoResponses(List.of(place));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).placeId()).isEqualTo(1L);
        assertThat(result.get(0).category()).isEqualTo("CULTURE");
    }


}