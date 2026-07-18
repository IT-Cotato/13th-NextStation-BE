package com.cotato.nextstation.domain.stamp.converter;

import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.stamp.dto.response.PopularCourseResponse;
import com.cotato.nextstation.domain.stamp.dto.response.StationPopularCoursesResponse;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class StampCourseConverter {

    public StationPopularCoursesResponse toStationPopularCoursesResponse(List<Course> courses) {
        List<PopularCourseResponse> responses = courses.stream()
                .map(this::toPopularCourseResponse)
                .toList();
        return new StationPopularCoursesResponse(responses);
    }

    private PopularCourseResponse toPopularCourseResponse(Course course) {
        return new PopularCourseResponse(
                course.getId(),
                course.getName(),
                course.getViewCount(),
                course.getSaveCount()
        );
    }
}