package com.cotato.nextstation.domain.course.service;

import com.cotato.nextstation.domain.course.converter.CourseConverter;
import com.cotato.nextstation.domain.course.dto.request.CourseCreateRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseNameUpdateRequest;
import com.cotato.nextstation.domain.course.dto.request.CoursePlaceOrderUpdateRequest;
import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.course.entity.CoursePlace;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.repository.CoursePlaceRepository;
import com.cotato.nextstation.domain.course.repository.CourseRepository;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseCommandServiceTest {

    @InjectMocks
    private CourseCommandService courseCommandService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CoursePlaceRepository coursePlaceRepository;

    @Mock
    private CourseConverter courseConverter;

    private Course course(String name) {
        return Course.builder().memberId(1L).stationId(100L).name(name).build();
    }

    private CoursePlace coursePlace(Long placeId, int orderNum) {
        return CoursePlace.builder().courseId(1L).placeId(placeId).orderNum(orderNum).build();
    }

    @Test
    @DisplayName("코스를 생성하면 코스와 장소들이 저장된다")
    void createCourse_success() {
        // given
        CourseCreateRequest request = new CourseCreateRequest("성수 코스", 100L, null, List.of(10L, 20L, 30L));
        Course saved = course("성수 코스");
        List<CoursePlace> places = List.of(coursePlace(10L, 1), coursePlace(20L, 2), coursePlace(30L, 3));
        given(courseConverter.toCourse(1L, request)).willReturn(saved);
        given(courseRepository.save(saved)).willReturn(saved);
        given(courseConverter.toCoursePlaces(saved.getId(), request.placeIds())).willReturn(places);
        given(coursePlaceRepository.saveAll(places)).willReturn(places);

        // when
        courseCommandService.createCourse(1L, request);

        // then
        verify(courseRepository).save(saved);
        verify(coursePlaceRepository).saveAll(places);
        verify(courseConverter).toResponse(saved, places);
    }

    @Test
    @DisplayName("같은 장소가 중복된 요청으로 코스를 생성하면 예외가 발생하고 저장하지 않는다")
    void createCourse_duplicatePlaces() {
        // given
        CourseCreateRequest request = new CourseCreateRequest("성수 코스", 100L, null, List.of(10L, 10L, 20L));

        // when & then
        assertThatThrownBy(() -> courseCommandService.createCourse(1L, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.INVALID_COURSE_PLACES.getMessage());
        verify(courseRepository, never()).save(any());
        verify(coursePlaceRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("본인 코스의 이름을 수정한다")
    void updateCourseName_success() {
        // given
        Course course = course("이전 이름");
        given(courseRepository.findByIdAndMemberIdAndIsDeletedFalse(1L, 1L)).willReturn(Optional.of(course));
        given(coursePlaceRepository.findByCourseIdOrderByOrderNumAsc(1L)).willReturn(List.of());

        // when
        courseCommandService.updateCourseName(1L, 1L, new CourseNameUpdateRequest("새 이름"));

        // then
        assertThat(course.getName()).isEqualTo("새 이름");
    }

    @Test
    @DisplayName("없거나 타인 소유 코스의 이름을 수정하면 예외가 발생한다")
    void updateCourseName_notFound() {
        // given
        given(courseRepository.findByIdAndMemberIdAndIsDeletedFalse(1L, 1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> courseCommandService.updateCourseName(1L, 1L, new CourseNameUpdateRequest("새 이름")))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("요청 순서대로 장소의 order_num이 재부여된다")
    void updateCoursePlaceOrder_success() {
        // given
        Course course = course("코스");
        CoursePlace place10 = coursePlace(10L, 1);
        CoursePlace place20 = coursePlace(20L, 2);
        CoursePlace place30 = coursePlace(30L, 3);
        List<CoursePlace> places = List.of(place10, place20, place30);
        given(courseRepository.findByIdAndMemberIdAndIsDeletedFalse(1L, 1L)).willReturn(Optional.of(course));
        given(coursePlaceRepository.findByCourseIdOrderByOrderNumAsc(1L)).willReturn(places);

        // when
        courseCommandService.updateCoursePlaceOrder(1L, 1L, new CoursePlaceOrderUpdateRequest(List.of(30L, 10L, 20L)));

        // then
        assertThat(place30.getOrderNum()).isEqualTo(1);
        assertThat(place10.getOrderNum()).isEqualTo(2);
        assertThat(place20.getOrderNum()).isEqualTo(3);
    }

    @Test
    @DisplayName("코스에 없는 장소로 순서를 수정하면 예외가 발생한다")
    void updateCoursePlaceOrder_mismatch() {
        // given
        Course course = course("코스");
        List<CoursePlace> places = List.of(coursePlace(10L, 1), coursePlace(20L, 2), coursePlace(30L, 3));
        given(courseRepository.findByIdAndMemberIdAndIsDeletedFalse(1L, 1L)).willReturn(Optional.of(course));
        given(coursePlaceRepository.findByCourseIdOrderByOrderNumAsc(1L)).willReturn(places);

        // when & then
        assertThatThrownBy(() -> courseCommandService.updateCoursePlaceOrder(
                1L, 1L, new CoursePlaceOrderUpdateRequest(List.of(10L, 20L, 99L))))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.INVALID_COURSE_PLACES.getMessage());
    }
}
