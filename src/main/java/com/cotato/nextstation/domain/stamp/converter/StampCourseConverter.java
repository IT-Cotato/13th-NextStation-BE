package com.cotato.nextstation.domain.stamp.converter;

import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.stamp.dto.response.PopularCourseResponse;
import com.cotato.nextstation.domain.stamp.dto.response.StationPopularCoursesResponse;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class StampCourseConverter {

    // TODO: Station 도메인 구현 완료 후 stationName, line 값을 채워서 반환하도록 수정
    public StationPopularCoursesResponse toStationPopularCoursesResponse(List<Course> courses) {
        List<PopularCourseResponse> responses = courses.stream()
                .map(this::toPopularCourseResponse)
                .toList();
        return new StationPopularCoursesResponse(null, null, responses);
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