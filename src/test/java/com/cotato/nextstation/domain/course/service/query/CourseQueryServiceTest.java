package com.cotato.nextstation.domain.course.service.query;

import com.cotato.nextstation.domain.course.converter.CourseConverter;
import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.PopularCourseResponse;
import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.course.entity.CoursePlace;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.dto.response.PlaceCourseResponse;
import com.cotato.nextstation.domain.course.repository.CoursePlaceRepository;
import com.cotato.nextstation.domain.course.repository.CourseRepository;
import com.cotato.nextstation.domain.course.repository.CourseRepository.PlaceCourseView;
import com.cotato.nextstation.domain.course.repository.CourseSaveRepository;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.service.query.PlaceInfoQueryService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private CourseSaveRepository courseSaveRepository;

    @Mock
    private PlaceInfoQueryService placeInfoQueryService;

    @Mock
    private CourseConverter courseConverter;

    private Course course(String name) {
        return Course.builder().memberId(1L).stationId(100L).name(name).build();
    }

    // ---------- 장소를 포함한 코스 ----------

    // 프로젝션은 인터페이스라 mock으로 만든다. 스터빙이 들어 있어 given(...) 밖에서 미리 생성한다.
    private PlaceCourseView placeCourseView(Long courseId) {
        PlaceCourseView view = mock(PlaceCourseView.class);
        lenient().when(view.getCourseId()).thenReturn(courseId);
        return view;
    }

    private CoursePlace coursePlace(Long courseId, Long placeId, int orderNum) {
        return CoursePlace.builder().courseId(courseId).placeId(placeId).orderNum(orderNum).build();
    }

    @Test
    @DisplayName("장소를 포함한 코스는 인기순 상위 6개만 조회한다")
    void getCoursesByPlace_limitsToSix() {
        // given
        given(courseRepository.findPopularPublicCoursesByPlaceId(eq(1L), any(Pageable.class)))
                .willReturn(List.of());

        // when
        List<PlaceCourseResponse> result = courseQueryService.getCoursesByPlace(1L);

        // then: 더보기가 없는 화면이라 6개 고정으로 요청한다
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(courseRepository).findPopularPublicCoursesByPlaceId(eq(1L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 6));
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("코스가 없으면 장소 조회를 하지 않고 빈 목록을 반환한다")
    void getCoursesByPlace_noCourses() {
        // given
        given(courseRepository.findPopularPublicCoursesByPlaceId(eq(1L), any(Pageable.class)))
                .willReturn(List.of());

        // when
        courseQueryService.getCoursesByPlace(1L);

        // then: 빈 id 목록으로 장소·태그를 조회하는 낭비를 막는다
        verify(coursePlaceRepository, never()).findByCourseIdInOrderByCourseIdAscOrderNumAsc(any());
        verify(placeInfoQueryService, never()).getTopTagNames(any());
    }

    @Test
    @DisplayName("코스별 장소 수와 대표 태그를 함께 내려준다")
    void getCoursesByPlace_placeCountAndTags() {
        // given: 10번 코스는 장소 3개, 20번 코스는 장소 2개
        PlaceCourseView view10 = placeCourseView(10L);
        PlaceCourseView view20 = placeCourseView(20L);
        given(courseRepository.findPopularPublicCoursesByPlaceId(eq(1L), any(Pageable.class)))
                .willReturn(List.of(view10, view20));
        given(coursePlaceRepository.findByCourseIdInOrderByCourseIdAscOrderNumAsc(any()))
                .willReturn(List.of(
                        coursePlace(10L, 100L, 1), coursePlace(10L, 101L, 2), coursePlace(10L, 102L, 3),
                        coursePlace(20L, 200L, 1), coursePlace(20L, 201L, 2)));
        given(placeInfoQueryService.getPlaceInfos(any())).willReturn(List.of());
        given(placeInfoQueryService.getTopTagNames(List.of(100L, 101L, 102L)))
                .willReturn(List.of("자연과함께", "사진찍기좋은", "가성비"));
        given(placeInfoQueryService.getTopTagNames(List.of(200L, 201L)))
                .willReturn(List.of("실내위주"));

        // when
        courseQueryService.getCoursesByPlace(1L);

        // then
        ArgumentCaptor<Integer> countCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<List<String>> tagsCaptor = ArgumentCaptor.forClass(List.class);
        verify(courseConverter, times(2))
                .toPlaceCourseResponse(any(), countCaptor.capture(), tagsCaptor.capture(), any());
        // 노출 순서를 섞으므로 두 코스가 어떤 순서로 들어와도 되도록 쌍으로 확인한다
        assertThat(countCaptor.getAllValues()).containsExactlyInAnyOrder(3, 2);
        // 카드에는 태그를 2개까지만 노출한다
        assertThat(tagsCaptor.getAllValues())
                .containsExactlyInAnyOrder(List.of("자연과함께", "사진찍기좋은"), List.of("실내위주"));
    }

    @Test
    @DisplayName("카드 배경은 코스의 첫 번째 장소 이미지를 쓴다")
    void getCoursesByPlace_coverImage() {
        // given: 10번 코스의 첫 장소는 order_num이 가장 작은 100번
        PlaceCourseView view10 = placeCourseView(10L);
        given(courseRepository.findPopularPublicCoursesByPlaceId(eq(1L), any(Pageable.class)))
                .willReturn(List.of(view10));
        given(coursePlaceRepository.findByCourseIdInOrderByCourseIdAscOrderNumAsc(any()))
                .willReturn(List.of(coursePlace(10L, 100L, 1), coursePlace(10L, 101L, 2)));
        given(placeInfoQueryService.getPlaceInfos(List.of(100L))).willReturn(List.of(
                new PlaceInfoResponse(100L, "보문골한옥집", "설명", "FOOD", "식당", "cover.jpg", 127.0, 37.5)));
        given(placeInfoQueryService.getTopTagNames(any())).willReturn(List.of());

        // when
        courseQueryService.getCoursesByPlace(1L);

        // then: 두 번째 장소가 아니라 첫 번째 장소의 이미지를 조회해 넘긴다
        ArgumentCaptor<String> imageCaptor = ArgumentCaptor.forClass(String.class);
        verify(courseConverter).toPlaceCourseResponse(any(), anyInt(), any(), imageCaptor.capture());
        assertThat(imageCaptor.getValue()).isEqualTo("cover.jpg");
    }

    @Test
    @DisplayName("장소 이미지가 없으면 배경 이미지는 null로 내려간다")
    void getCoursesByPlace_noCoverImage() {
        // given: 이미지가 아직 없는 장소 (장소 이미지·카테고리 기본 이미지 모두 없음)
        PlaceCourseView view10 = placeCourseView(10L);
        given(courseRepository.findPopularPublicCoursesByPlaceId(eq(1L), any(Pageable.class)))
                .willReturn(List.of(view10));
        given(coursePlaceRepository.findByCourseIdInOrderByCourseIdAscOrderNumAsc(any()))
                .willReturn(List.of(coursePlace(10L, 100L, 1)));
        given(placeInfoQueryService.getPlaceInfos(List.of(100L))).willReturn(List.of(
                new PlaceInfoResponse(100L, "보문골한옥집", "설명", "FOOD", "식당", null, 127.0, 37.5)));
        given(placeInfoQueryService.getTopTagNames(any())).willReturn(List.of());

        // when
        courseQueryService.getCoursesByPlace(1L);

        // then
        ArgumentCaptor<String> imageCaptor = ArgumentCaptor.forClass(String.class);
        verify(courseConverter).toPlaceCourseResponse(any(), anyInt(), any(), imageCaptor.capture());
        assertThat(imageCaptor.getValue()).isNull();
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
                new PopularCourseResponse(1L, "보문역 코스", 300, 128, false),
                new PopularCourseResponse(2L, "성수 코스", 200, 50, false));
        given(courseRepository.findPopularPublicCoursesByStationId(eq(6L), any(Pageable.class))).willReturn(courses);
        given(courseConverter.toPopularResponses(eq(courses), any())).willReturn(responses);

        // when
        List<PopularCourseResponse> result = courseQueryService.getPopularCoursesByStation(6L, 3);

        // then
        assertThat(result).isEqualTo(responses);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(courseRepository).findPopularPublicCoursesByStationId(eq(6L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 3));
    }
}
