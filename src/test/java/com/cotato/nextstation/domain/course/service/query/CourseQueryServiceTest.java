package com.cotato.nextstation.domain.course.service.query;

import com.cotato.nextstation.domain.course.converter.CourseConverter;
import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.PopularCourseResponse;
import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.course.entity.CoursePlace;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.repository.CoursePlaceRepository;
import com.cotato.nextstation.domain.course.repository.CourseRepository;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseQueryServiceTest {

    @InjectMocks
    private CourseQueryService courseQueryService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CoursePlaceRepository coursePlaceRepository;

    @Mock
    private CourseConverter courseConverter;

    private Course course(String name) {
        return Course.builder().memberId(1L).stationId(100L).name(name).build();
    }

    @Test
    @DisplayName("코스 정보를 조회하면 CourseInfoResponse를 반환한다")
    void getCourseInfo_success() {
        // given
        Course course = course("성수 코스");
        CourseInfoResponse response = new CourseInfoResponse(
                1L, "성수 코스", 1L, 100L, null, 0, 0, course.getCreatedAt());
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));
        given(courseConverter.toInfoResponse(course)).willReturn(response);

        // when
        CourseInfoResponse result = courseQueryService.getCourseInfo(1L);

        // then
        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("없는 코스의 정보를 조회하면 예외가 발생한다")
    void getCourseInfo_notFound() {
        // given
        given(courseRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> courseQueryService.getCourseInfo(1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("코스의 장소 목록을 순서대로 조회한다")
    void getCoursePlaces_success() {
        // given
        Course course = course("성수 코스");
        List<CoursePlace> coursePlaces = List.of(
                CoursePlace.builder().courseId(1L).placeId(10L).orderNum(1).build(),
                CoursePlace.builder().courseId(1L).placeId(20L).orderNum(2).build()
        );
        List<CoursePlaceInfoResponse> responses = List.of(
                new CoursePlaceInfoResponse(10L, 1), new CoursePlaceInfoResponse(20L, 2));
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));
        given(coursePlaceRepository.findByCourseIdOrderByOrderNumAsc(1L)).willReturn(coursePlaces);
        given(courseConverter.toPlaceInfoResponses(coursePlaces)).willReturn(responses);

        // when
        List<CoursePlaceInfoResponse> result = courseQueryService.getCoursePlaces(1L);

        // then
        assertThat(result).isEqualTo(responses);
    }

    @Test
    @DisplayName("없는 코스의 장소 목록을 조회하면 예외가 발생한다")
    void getCoursePlaces_notFound() {
        // given
        given(courseRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> courseQueryService.getCoursePlaces(1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("역별 인기 코스를 limit 개수만큼 조회해 변환 결과를 반환한다")
    void getPopularCoursesByStation_success() {
        // 인기순 필터/정렬 자체는 리포지토리 쿼리 책임이고,
        // 여기서는 서비스가 limit을 Pageable로 넘기고 변환 결과를 반환하는지 확인한다.
        List<Course> courses = List.of(course("보문역 코스"), course("성수 코스"));
        List<PopularCourseResponse> responses = List.of(
                new PopularCourseResponse(1L, "보문역 코스", 300, 128),
                new PopularCourseResponse(2L, "성수 코스", 200, 50));
        given(courseRepository.findPopularPublicCoursesByStationId(eq(6L), any(Pageable.class))).willReturn(courses);
        given(courseConverter.toPopularResponses(courses)).willReturn(responses);

        // when
        List<PopularCourseResponse> result = courseQueryService.getPopularCoursesByStation(6L, 3);

        // then
        assertThat(result).isEqualTo(responses);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(courseRepository).findPopularPublicCoursesByStationId(eq(6L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 3));
    }
}
