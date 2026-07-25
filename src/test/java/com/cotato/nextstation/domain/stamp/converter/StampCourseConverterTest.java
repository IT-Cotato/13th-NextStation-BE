package com.cotato.nextstation.domain.stamp.converter;

import com.cotato.nextstation.domain.course.dto.response.PopularCourseResponse;
import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.stamp.dto.response.StationPopularCoursesResponse;
import com.cotato.nextstation.domain.station.entity.Station;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class StampCourseConverterTest {

    private final StampCourseConverter stampCourseConverter = new StampCourseConverter();

    @Test
    @DisplayName("Station 정보와 코스 목록을 응답으로 변환한다")
    void toStationPopularCoursesResponse_success() {
        // given
        Station station = mock(Station.class);
        given(station.getStationName()).willReturn("보문역");

        String lineName = "6호선";

        List<PopularCourseResponse> courses = List.of(
                new PopularCourseResponse(1L, "보문역 코스", 300, 128, false)
        );

        // when
        StationPopularCoursesResponse response = stampCourseConverter
                .toStationPopularCoursesResponse(station, lineName, courses);

        // then
        assertThat(response.stationName()).isEqualTo("보문역");
        assertThat(response.line()).isEqualTo("6호선");
        assertThat(response.courses()).hasSize(1);
        assertThat(response.courses().get(0).courseId()).isEqualTo(1L);
        assertThat(response.courses().get(0).name()).isEqualTo("보문역 코스");
        assertThat(response.courses().get(0).viewCount()).isEqualTo(300);
        assertThat(response.courses().get(0).likeCount()).isEqualTo(128);
    }

    @Test
    @DisplayName("노선 정보가 없으면 line이 null로 반환된다")
    void toStationPopularCoursesResponse_nullLine() {
        // given
        Station station = mock(Station.class);
        given(station.getStationName()).willReturn("보문역");

        // when
        StationPopularCoursesResponse response = stampCourseConverter
                .toStationPopularCoursesResponse(station, null, List.of());

        // then
        assertThat(response.stationName()).isEqualTo("보문역");
        assertThat(response.line()).isNull();
        assertThat(response.courses()).isEmpty();
    }


    @Test
    @DisplayName("빈 목록이면 빈 courses를 반환한다")
    void toStationPopularCoursesResponse_empty() {
        // given
        Station station = mock(Station.class);
        given(station.getStationName()).willReturn("보문역");

        // when
        StationPopularCoursesResponse response = stampCourseConverter
                .toStationPopularCoursesResponse(station, "6호선", List.of());

        // then
        assertThat(response.courses()).isEmpty();
    }
}