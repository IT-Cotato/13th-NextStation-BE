package com.cotato.nextstation.domain.stamp.service.query;

import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.stamp.converter.StampCourseConverter;
import com.cotato.nextstation.domain.stamp.dto.response.PopularCourseResponse;
import com.cotato.nextstation.domain.stamp.dto.response.StationPopularCoursesResponse;
import com.cotato.nextstation.domain.stamp.repository.StampCourseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StampCourseQueryServiceTest {

    @InjectMocks
    private StampCourseQueryService stampCourseQueryService;

    @Mock
    private StampCourseRepository stampCourseRepository;
    @Mock
    private StampCourseConverter stampCourseConverter;

    @Test
    @DisplayName("역별 인기 코스를 상위 3개까지 조회한다")
    void getPopularCoursesByStation_success() {
        // given
        Long stationId = 12L;
        Course course1 = mock(Course.class);
        Course course2 = mock(Course.class);
        List<Course> courses = List.of(course1, course2);

        given(stampCourseRepository.findPopularCoursesByStationId(eq(stationId), any()))
                .willReturn(courses);

        StationPopularCoursesResponse expected = new StationPopularCoursesResponse(
                null, null, List.of(
                new PopularCourseResponse(1L, "보문역 코스", 300, 128)
        ));
        given(stampCourseConverter.toStationPopularCoursesResponse(courses))
                .willReturn(expected);

        // when
        StationPopularCoursesResponse result = stampCourseQueryService.getPopularCoursesByStation(stationId);

        // then
        assertThat(result).isEqualTo(expected);
        verify(stampCourseRepository).findPopularCoursesByStationId(eq(stationId), any());
    }

    @Test
    @DisplayName("인기 코스가 없으면 빈 목록을 반환한다")
    void getPopularCoursesByStation_empty() {
        // given
        Long stationId = 999L;
        given(stampCourseRepository.findPopularCoursesByStationId(eq(stationId), any()))
                .willReturn(List.of());

        StationPopularCoursesResponse expected = new StationPopularCoursesResponse(null, null, List.of());
        given(stampCourseConverter.toStationPopularCoursesResponse(List.of()))
                .willReturn(expected);

        // when
        StationPopularCoursesResponse result = stampCourseQueryService.getPopularCoursesByStation(stationId);

        // then
        assertThat(result.courses()).isEmpty();
    }
}