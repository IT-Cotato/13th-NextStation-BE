package com.cotato.nextstation.domain.course.converter;

import com.cotato.nextstation.domain.course.dto.request.CourseCreateRequest;
import com.cotato.nextstation.domain.course.dto.response.CourseCreateResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseNameResponse;
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

    public CourseCreateResponse toCreateResponse(Course course) {
        return new CourseCreateResponse(course.getId(), course.getName(), course.getCreatedAt());
    }

    public CourseNameResponse toNameResponse(Course course) {
        return new CourseNameResponse(course.getId(), course.getName());
    }
}
