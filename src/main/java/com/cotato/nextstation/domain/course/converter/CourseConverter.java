package com.cotato.nextstation.domain.course.converter;

import com.cotato.nextstation.domain.course.dto.request.CourseCreateRequest;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseResponse;
import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.course.entity.CoursePlace;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
public class CourseConverter {

    public Course toCourse(Long memberId, CourseCreateRequest request) {
        return Course.builder()
                .memberId(memberId)
                .stationId(request.stationId())
                .conceptTourId(request.conceptTourId())
                .name(request.name())
                .build();
    }

    public List<CoursePlace> toCoursePlaces(Long courseId, List<Long> placeIds) {
        return IntStream.range(0, placeIds.size())
                .mapToObj(index -> CoursePlace.builder()
                        .courseId(courseId)
                        .placeId(placeIds.get(index))
                        .orderNum(index + 1)
                        .build())
                .toList();
    }

    public CourseResponse toResponse(Course course, List<CoursePlace> coursePlaces) {
        return new CourseResponse(
                course.getId(),
                course.getName(),
                course.getStationId(),
                course.getConceptTourId(),
                course.getJournalId(),
                course.getViewCount(),
                course.getSaveCount(),
                course.getCreatedAt(),
                toPlaceResponses(coursePlaces)
        );
    }

    private List<CoursePlaceResponse> toPlaceResponses(List<CoursePlace> coursePlaces) {
        return coursePlaces.stream()
                .map(coursePlace -> new CoursePlaceResponse(coursePlace.getPlaceId(), coursePlace.getOrderNum()))
                .toList();
    }
}
