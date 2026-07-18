package com.cotato.nextstation.domain.stamp.service.query;

import com.cotato.nextstation.domain.stamp.dto.response.StationPopularCoursesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StampCourseQueryService {

    private static final int POPULAR_COURSE_LIMIT = 3;

    // TODO: CourseQueryService에 stationId 기준 인기 코스 조회 메서드 제공받으면 주입
    // private final CourseQueryService courseQueryService;

    public StationPopularCoursesResponse getPopularCoursesByStation(Long stationId) {
        // TODO: courseQueryService.getPopularCoursesByStation(stationId, POPULAR_COURSE_LIMIT) 호출로 교체
        throw new UnsupportedOperationException("Course 도메인 연동 대기 중");
    }
}