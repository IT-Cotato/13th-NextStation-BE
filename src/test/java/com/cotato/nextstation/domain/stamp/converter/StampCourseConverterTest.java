package com.cotato.nextstation.domain.stamp.converter;

import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.stamp.dto.response.StationPopularCoursesResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class StampCourseConverterTest {

    private final StampCourseConverter stampCourseConverter = new StampCourseConverter();

    @Test
    @DisplayName("Course 목록을 PopularCourseResponse 목록으로 변환한다")
    void toStationPopularCoursesResponse_success() {
        // given
        Course course = mock(Course.class);
        given(course.getId()).willReturn(1L);
        given(course.getName()).willReturn("보문역 코스");
        given(course.getViewCount()).willReturn(300);
        given(course.getSaveCount()).willReturn(128);

        // when
        StationPopularCoursesResponse response = stampCourseConverter.toStationPopularCoursesResponse(List.of(course));

        // then
        assertThat(response.stationName()).isNull();
        assertThat(response.line()).isNull();
        assertThat(response.courses()).hasSize(1);
        assertThat(response.courses().get(0).courseId()).isEqualTo(1L);
        assertThat(response.courses().get(0).name()).isEqualTo("보문역 코스");
        assertThat(response.courses().get(0).viewCount()).isEqualTo(300);
        assertThat(response.courses().get(0).saveCount()).isEqualTo(128);
    }

    @Test
    @DisplayName("빈 목록이면 빈 courses를 반환한다")
    void toStationPopularCoursesResponse_empty() {
        // when
        StationPopularCoursesResponse response = stampCourseConverter.toStationPopularCoursesResponse(List.of());

        // then
        assertThat(response.courses()).isEmpty();
    }
}