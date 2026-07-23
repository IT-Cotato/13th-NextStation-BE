package com.cotato.nextstation.domain.recommendation.converter;

import com.cotato.nextstation.domain.recommendation.dto.response.CoursePreviewPlaceResponse;
import com.cotato.nextstation.domain.recommendation.dto.response.CoursePreviewResponse;
import com.cotato.nextstation.domain.recommendation.dto.response.RandomRecommendationResponse;
import com.cotato.nextstation.domain.recommendation.dto.response.RecommendedStationResponse;
import com.cotato.nextstation.domain.recommendation.service.port.StationPlaceView;
import com.cotato.nextstation.domain.station.entity.Line;
import com.cotato.nextstation.domain.station.entity.Station;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RecommendationConverter {

    public RandomRecommendationResponse toRandomResponse(Station station, String courseName, List<StationPlaceView> places) {
        return new RandomRecommendationResponse(
                toRecommendedStation(station),
                toCoursePreview(courseName, places)
        );
    }

    private RecommendedStationResponse toRecommendedStation(Station station) {
        Line drawLine = station.getDrawLine();
        return new RecommendedStationResponse(
                station.getId(),
                station.getStationName(),
                station.getDescription(),
                station.getTodo(),
                drawLine != null ? drawLine.getName() : null
        );
    }

    private CoursePreviewResponse toCoursePreview(String courseName, List<StationPlaceView> places) {
        List<CoursePreviewPlaceResponse> placeResponses = places.stream()
                .map(this::toCoursePreviewPlace)
                .toList();
        return new CoursePreviewResponse(courseName, placeResponses);
    }

    private CoursePreviewPlaceResponse toCoursePreviewPlace(StationPlaceView view) {
        return new CoursePreviewPlaceResponse(
                view.placeId(),
                view.placeName(),
                view.description(),
                view.categoryCode(),
                view.categoryName(),
                view.imageUrl(),
                view.xCoordinate(),
                view.yCoordinate()
        );
    }
}
