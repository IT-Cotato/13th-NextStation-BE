package com.cotato.nextstation.domain.station.converter;

import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.dto.response.StationPlaceCategoryResponse;
import com.cotato.nextstation.domain.station.dto.response.StationPlaceResponse;
import com.cotato.nextstation.domain.station.dto.response.StationPlacesResponse;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.entity.Line;
import com.cotato.nextstation.domain.station.entity.Station;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StationConverter {

    private final LineConverter lineConverter;

    public StationSummaryResponse toSummaryResponse(Station station, List<LineSummaryResponse> lines) {
        return new StationSummaryResponse(station.getId(), station.getStationName(), lines);
    }

    public StationPlacesResponse toPlacesResponse(Station station, List<LineSummaryResponse> lines, List<String> tags,
                                                  String defaultCourseName,
                                                  List<StationPlaceCategoryResponse> categories) {
        Line drawLine = station.getDrawLine();
        return new StationPlacesResponse(
                station.getId(),
                station.getStationName(),
                station.getDescription(),
                drawLine != null ? lineConverter.toSummaryResponse(drawLine) : null,
                lines,
                tags,
                defaultCourseName,
                categories
        );
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
