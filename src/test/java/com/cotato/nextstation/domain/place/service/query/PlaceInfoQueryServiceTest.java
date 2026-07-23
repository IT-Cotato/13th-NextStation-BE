package com.cotato.nextstation.domain.place.service.query;

import com.cotato.nextstation.domain.place.converter.PlaceConverter;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.entity.Place;
import com.cotato.nextstation.domain.place.entity.PlaceTag;
import com.cotato.nextstation.domain.place.entity.PlaceTagMapping;
import com.cotato.nextstation.domain.place.enums.PlaceTagName;
import com.cotato.nextstation.domain.place.repository.PlaceRepository;
import com.cotato.nextstation.domain.place.repository.PlaceTagMappingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PlaceInfoQueryServiceTest {

    @InjectMocks
    private PlaceInfoQueryService placeInfoQueryService;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceTagMappingRepository placeTagMappingRepository;

    @Mock
    private PlaceConverter placeConverter;

    @Test
    @DisplayName("placeIds로 장소 정보를 일괄 조회한다")
    void getPlaceInfos_success() {
        // given
        List<Long> placeIds = List.of(1L, 2L);
        List<Place> places = List.of(mock(Place.class), mock(Place.class));
        List<PlaceInfoResponse> expected = List.of(
                new PlaceInfoResponse(1L, "보문숲길도서관", "설명", "CULTURE", "문화공간", "https://image.jpg", 127.1, 37.5)
        );

        given(placeRepository.findAllById(placeIds)).willReturn(places);
        given(placeConverter.toPlaceInfoResponses(places)).willReturn(expected);

        // when
        List<PlaceInfoResponse> result = placeInfoQueryService.getPlaceInfos(placeIds);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("태그가 많은 순서대로 상위 3개만 반환한다")
    void getTopTagNames_top3() {
        // given
        List<Long> placeIds = List.of(1L, 2L);

        List<PlaceTagMapping> mappings = List.of(
                mapping(PlaceTagName.NATURE),
                mapping(PlaceTagName.NATURE),
                mapping(PlaceTagName.NATURE),
                mapping(PlaceTagName.BUDGET),
                mapping(PlaceTagName.BUDGET),
                mapping(PlaceTagName.SHOPPING),
                mapping(PlaceTagName.INDOOR)
        );

        given(placeTagMappingRepository.findByPlaceIdIn(placeIds)).willReturn(mappings);

        // when
        List<String> result = placeInfoQueryService.getTopTagNames(placeIds);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo("NATURE");   // count=3, 확정 1위
        assertThat(result.get(1)).isEqualTo("BUDGET");    // count=2, 확정 2위
        assertThat(result.get(2)).isIn("SHOPPING", "INDOOR");  // count=1 동점, 둘 중 하나
    }

    @Test
    @DisplayName("태그가 3개 이하면 있는 만큼만 반환한다")
    void getTopTagNames_lessThanLimit() {
        // given
        List<Long> placeIds = List.of(1L);
        List<PlaceTagMapping> mappings = List.of(mapping(PlaceTagName.NATURE));

        given(placeTagMappingRepository.findByPlaceIdIn(placeIds)).willReturn(mappings);

        // when
        List<String> result = placeInfoQueryService.getTopTagNames(placeIds);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("NATURE");
    }

    private PlaceTagMapping mapping(PlaceTagName tagName) {
        PlaceTag placeTag = mock(PlaceTag.class);
        given(placeTag.getName()).willReturn(tagName);

        PlaceTagMapping mapping = mock(PlaceTagMapping.class);
        given(mapping.getPlaceTag()).willReturn(placeTag);
        return mapping;
    }
}