package com.cotato.nextstation.domain.stamp.service.query;

import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.stamp.converter.StampCourseConverter;
import com.cotato.nextstation.domain.stamp.dto.response.StationPopularCoursesResponse;
import com.cotato.nextstation.domain.stamp.repository.StampCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

// TODO: Course 도메인 구현 완료 후 CourseQueryService 경유 방식으로 리팩터링 예정
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StampCourseQueryService {

    private static final int POPULAR_COURSE_LIMIT = 3;

    private final StampCourseRepository stampCourseRepository;
    private final StampCourseConverter stampCourseConverter;

    public StationPopularCoursesResponse getPopularCoursesByStation(Long stationId) {
        List<Course> courses = stampCourseRepository.findPopularCoursesByStationId(
                stationId, PageRequest.of(0, POPULAR_COURSE_LIMIT)
        );
        return stampCourseConverter.toStationPopularCoursesResponse(courses);
    }
}