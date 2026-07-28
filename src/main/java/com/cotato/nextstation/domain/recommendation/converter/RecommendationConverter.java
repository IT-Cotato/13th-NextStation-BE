package com.cotato.nextstation.domain.recommendation.converter;

import com.cotato.nextstation.domain.recommendation.dto.response.CoursePreviewPlaceResponse;
import com.cotato.nextstation.domain.recommendation.dto.response.CoursePreviewResponse;
import com.cotato.nextstation.domain.recommendation.dto.response.RandomRecommendationResponse;
import com.cotato.nextstation.domain.recommendation.dto.response.RecommendedStationResponse;
import com.cotato.nextstation.domain.recommendation.service.port.StationPlaceView;
import com.cotato.nextstation.domain.station.converter.LineConverter;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.Line;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.repository.StationLineRepository.StationLineView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class RecommendationConverter {

    // station.todo에 "1. 산책하기\n2. 시장 구경하기"처럼 번호가 붙은 채로 저장돼 있어 항목별로 나눌 때 떼어낸다.
    private static final Pattern TODO_NUMBER_PREFIX = Pattern.compile("^\\d+[.)]\\s*");

    private final LineConverter lineConverter;

    public RandomRecommendationResponse toRandomResponse(Station station, List<StationLineView> lineViews,
                                                         String courseName, List<StationPlaceView> places) {
        return new RandomRecommendationResponse(
                toRecommendedStation(station, lineViews),
                toCoursePreview(courseName, places)
        );
    }

    private RecommendedStationResponse toRecommendedStation(Station station, List<StationLineView> lineViews) {
        return new RecommendedStationResponse(
                station.getId(),
                station.getStationName(),
                station.getDescription(),
                splitTodos(station.getTodo()),
                toLinesWithDrawLineFirst(lineViews, station.getDrawLine())
        );
    }

    // 결과 화면 노선 칩은 대표 노선을 맨 앞에 두고, 나머지는 조회 순서(노선 ID 순)를 유지한다.
    private List<LineSummaryResponse> toLinesWithDrawLineFirst(List<StationLineView> lineViews, Line drawLine) {
        List<LineSummaryResponse> lines = lineViews.stream()
                .map(lineConverter::toSummaryResponse)
                .toList();
        if (drawLine == null) {
            return lines;
        }

        Long drawLineId = drawLine.getId();
        return lines.stream()
                .sorted(Comparator.comparing((LineSummaryResponse line) -> !line.id().equals(drawLineId)))
                .toList();
    }

    // 여러 줄로 저장된 할 일을 항목 목록으로 나눈다. 프론트가 번호를 직접 붙이므로 번호 접두사는 제거한다.
    private List<String> splitTodos(String todo) {
        if (todo == null || todo.isBlank()) {
            return List.of();
        }

        return todo.lines()
                .map(line -> TODO_NUMBER_PREFIX.matcher(line.strip()).replaceFirst("").strip())
                .filter(line -> !line.isEmpty())
                .toList();
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
