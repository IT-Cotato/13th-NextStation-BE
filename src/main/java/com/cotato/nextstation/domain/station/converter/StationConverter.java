package com.cotato.nextstation.domain.station.converter;

import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.station.dto.response.StationPlaceCategoryResponse;
import com.cotato.nextstation.domain.station.dto.response.StationPlaceResponse;
import com.cotato.nextstation.domain.station.dto.response.StationPlacesResponse;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.entity.Station;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StationConverter {

    public StationSummaryResponse toSummaryResponse(Station station, List<String> lines) {
        return new StationSummaryResponse(station.getId(), station.getStationName(), lines);
    }

    public StationPlacesResponse toPlacesResponse(Station station, String defaultCourseName,
                                                  List<StationPlaceCategoryResponse> categories) {
        return new StationPlacesResponse(station.getId(), station.getStationName(), defaultCourseName, categories);
    }

    public StationPlaceCategoryResponse toPlaceCategoryResponse(String categoryCode, String categoryName,
                                                                List<PlaceInfoResponse> places) {
        return new StationPlaceCategoryResponse(categoryCode, categoryName, places.stream()
                .map(this::toPlaceResponse)
                .toList());
    }

    private StationPlaceResponse toPlaceResponse(PlaceInfoResponse place) {
        return new StationPlaceResponse(
                place.placeId(),
                place.placeName(),
                place.description(),
                place.imageUrl(),
                place.xCoordinate(),
                place.yCoordinate()
        );
    }
}
