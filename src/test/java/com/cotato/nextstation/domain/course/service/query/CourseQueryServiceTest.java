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
import com.cotato.nextstation.domain.course.repository.CourseRepository.LineView;
import com.cotato.nextstation.domain.course.repository.CourseRepository.MyCourseView;
import com.cotato.nextstation.domain.course.repository.CourseRepository.PlaceCourseView;
import com.cotato.nextstation.domain.course.repository.CourseLikeRepository;
import com.cotato.nextstation.domain.course.repository.CourseLikeRepository.LikedCourseView;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.service.query.PlaceInfoQueryService;
import com.cotato.nextstation.domain.stamp.service.query.MemberStampQueryService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import com.cotato.nextstation.global.util.CursorData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private CourseLikeRepository courseLikeRepository;

    @Mock
    private PlaceInfoQueryService placeInfoQueryService;

    @Mock
    private MemberStampQueryService memberStampQueryService;

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

    // ---------- 저장 탭 - 좋아요한 코스 목록 ----------

    // 프로젝션은 인터페이스라 mock으로 만든다. 스터빙이 들어 있으므로 given(...) 밖에서 미리 생성한다.
    private LikedCourseView likedView(Long likeId, Long courseId, LocalDateTime likedAt) {
        LikedCourseView view = mock(LikedCourseView.class);
        lenient().when(view.getLikeId()).thenReturn(likeId);
        lenient().when(view.getCourseId()).thenReturn(courseId);
        lenient().when(view.getLikedAt()).thenReturn(likedAt);
        return view;
    }

    private MyCourseView myView(Long courseId, LocalDateTime createdAt) {
        MyCourseView view = mock(MyCourseView.class);
        lenient().when(view.getCourseId()).thenReturn(courseId);
        lenient().when(view.getCreatedAt()).thenReturn(createdAt);
        return view;
    }

    @Test
    @DisplayName("좋아요 목록은 요청 크기보다 1개 더 조회해 다음 페이지 여부를 판단하고, 초과분은 잘라낸다")
    void getLikedCourses_hasNext() {
        // given: size 2를 요청했는데 3개가 조회되면 다음 페이지가 있다는 뜻이다
        LocalDateTime likedAt = LocalDateTime.of(2026, 7, 23, 12, 0);
        List<LikedCourseView> views = List.of(
                likedView(30L, 3L, likedAt), likedView(20L, 2L, likedAt), likedView(10L, 1L, likedAt));
        given(courseLikeRepository.findLikedCourses(eq(1L), any(Pageable.class))).willReturn(views);

        // when
        courseQueryService.getLikedCourses(1L, null, 2);

        // then: 조회는 3개(=2+1), 응답에 담기는 건 2개
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(courseLikeRepository).findLikedCourses(eq(1L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 3));

        ArgumentCaptor<List<LikedCourseView>> contentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> cursorCaptor = ArgumentCaptor.forClass(String.class);
        verify(courseConverter).toLikedListResponse(contentCaptor.capture(), cursorCaptor.capture(), eq(true));
        assertThat(contentCaptor.getValue()).hasSize(2);

        // 다음 커서는 마지막 항목 기준(좋아요 시각 + course_like.id)으로 만들어진다
        CursorData nextCursor = CursorData.decode(cursorCaptor.getValue());
        assertThat(nextCursor.id()).isEqualTo(20L);
        assertThat(nextCursor.dateTimeValue()).isEqualTo(likedAt);
        assertThat(nextCursor.longValue()).isNull();
    }

    @Test
    @DisplayName("마지막 페이지면 다음 커서 없이 응답한다")
    void getLikedCourses_lastPage() {
        // given: likedView가 내부에서 스터빙하므로 given(...) 안에서 호출하면 중첩 스터빙이 된다. 미리 만들어 둔다.
        LikedCourseView view = likedView(10L, 1L, LocalDateTime.of(2026, 7, 23, 12, 0));
        given(courseLikeRepository.findLikedCourses(eq(1L), any(Pageable.class))).willReturn(List.of(view));

        // when
        courseQueryService.getLikedCourses(1L, null, 2);

        // then
        verify(courseConverter).toLikedListResponse(any(), eq(null), eq(false));
    }

    @Test
    @DisplayName("커서를 주면 그 시각 이후 페이지를 조회한다")
    void getLikedCourses_withCursor() {
        // given
        LocalDateTime likedAt = LocalDateTime.of(2026, 7, 23, 12, 0);
        String cursor = new CursorData(20L, null, likedAt).encode();
        given(courseLikeRepository.findLikedCoursesAfterCursor(eq(1L), eq(likedAt), eq(20L), any(Pageable.class)))
                .willReturn(List.of());

        // when
        courseQueryService.getLikedCourses(1L, cursor, 2);

        // then: 첫 페이지 쿼리는 타지 않는다
        verify(courseLikeRepository, never()).findLikedCourses(any(), any());
        verify(courseLikeRepository).findLikedCoursesAfterCursor(eq(1L), eq(likedAt), eq(20L), any(Pageable.class));
    }

    @Test
    @DisplayName("정렬 기준과 맞지 않는 커서면 예외가 발생한다")
    void getLikedCourses_invalidCursor() {
        // given: 시간순 목록인데 숫자 커서(인기순용)가 들어온 경우
        String cursor = new CursorData(20L, 100L, null).encode();

        // when & then
        assertThatThrownBy(() -> courseQueryService.getLikedCourses(1L, cursor, 2))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(GlobalErrorCode.INVALID_CURSOR.getMessage());
    }

    @Test
    @DisplayName("size가 허용 범위를 벗어나면 예외가 발생한다")
    void getLikedCourses_invalidSize() {
        assertThatThrownBy(() -> courseQueryService.getLikedCourses(1L, null, 0))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(GlobalErrorCode.INVALID_PAGE_SIZE.getMessage());
        assertThatThrownBy(() -> courseQueryService.getLikedCourses(1L, null, 51))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(GlobalErrorCode.INVALID_PAGE_SIZE.getMessage());
    }

    @Test
    @DisplayName("size를 생략하면 기본 크기로 조회한다")
    void getLikedCourses_defaultSize() {
        // given
        given(courseLikeRepository.findLikedCourses(eq(1L), any(Pageable.class))).willReturn(List.of());

        // when
        courseQueryService.getLikedCourses(1L, null, null);

        // then: 기본 10 + hasNext 판단용 1
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(courseLikeRepository).findLikedCourses(eq(1L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 11));
    }

    // ---------- 저장 탭 - 내가 만든 코스 목록 ----------

    @Test
    @DisplayName("내 코스 목록은 호선/역 필터를 그대로 조회 조건으로 넘긴다")
    void getMyCourses_passesFilters() {
        // given
        given(courseRepository.findMyCourses(eq(1L), eq(6L), eq(9L), any(Pageable.class))).willReturn(List.of());
        given(courseRepository.findAvailableLines(1L)).willReturn(List.of());

        // when
        courseQueryService.getMyCourses(1L, 6L, 9L, null, 10);

        // then
        verify(courseRepository).findMyCourses(eq(1L), eq(6L), eq(9L), any(Pageable.class));
    }

    @Test
    @DisplayName("필터를 주지 않으면 null로 넘겨 조건을 걸지 않는다")
    void getMyCourses_withoutFilters() {
        // given
        given(courseRepository.findMyCourses(eq(1L), eq(null), eq(null), any(Pageable.class))).willReturn(List.of());
        given(courseRepository.findAvailableLines(1L)).willReturn(List.of());

        // when
        courseQueryService.getMyCourses(1L, null, null, null, 10);

        // then
        verify(courseRepository).findMyCourses(eq(1L), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("선택 가능한 호선은 최초 조회에서만 계산한다")
    void getMyCourses_availableLinesOnlyOnFirstPage() {
        // given: 필터 칩은 화면에 한 번만 그리므로 다음 페이지에서는 다시 조회하지 않는다
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 23, 12, 0);
        String cursor = new CursorData(5L, null, createdAt).encode();
        given(courseRepository.findMyCoursesAfterCursor(eq(1L), any(), any(), eq(createdAt), eq(5L), any(Pageable.class)))
                .willReturn(List.of());

        // when
        courseQueryService.getMyCourses(1L, null, null, cursor, 10);

        // then
        verify(courseRepository, never()).findAvailableLines(any());
        ArgumentCaptor<List<LineView>> linesCaptor = ArgumentCaptor.forClass(List.class);
        verify(courseConverter).toMyListResponse(any(), any(), linesCaptor.capture(), any(), eq(false));
        assertThat(linesCaptor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("커서가 빈 문자열이면 최초 조회로 보고 선택 가능한 호선도 함께 내려준다")
    void getMyCourses_blankCursorIsFirstPage() {
        // given: 빈 문자열은 커서 없음으로 취급된다. 조회는 첫 페이지인데 칩만 빠지는 일이 없어야 한다.
        given(courseRepository.findMyCourses(eq(1L), any(), any(), any(Pageable.class))).willReturn(List.of());
        given(courseRepository.findAvailableLines(1L)).willReturn(List.of());

        // when
        courseQueryService.getMyCourses(1L, null, null, "", 10);

        // then
        verify(courseRepository).findMyCourses(eq(1L), any(), any(), any(Pageable.class));
        verify(courseRepository).findAvailableLines(1L);
    }

    @Test
    @DisplayName("내 코스 목록의 다음 커서는 마지막 코스의 생성 시각과 id로 만든다")
    void getMyCourses_nextCursor() {
        // given: myView가 내부에서 스터빙하므로 given(...) 안에서 호출하면 중첩 스터빙이 된다. 미리 만들어 둔다.
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 23, 12, 0);
        List<MyCourseView> views = List.of(myView(3L, createdAt), myView(2L, createdAt));
        given(courseRepository.findMyCourses(eq(1L), any(), any(), any(Pageable.class))).willReturn(views);
        given(courseRepository.findAvailableLines(1L)).willReturn(List.of());

        // when: size 1 요청 → 2개 조회되어 다음 페이지 있음
        courseQueryService.getMyCourses(1L, null, null, null, 1);

        // then
        ArgumentCaptor<String> cursorCaptor = ArgumentCaptor.forClass(String.class);
        verify(courseConverter).toMyListResponse(any(), any(), any(), cursorCaptor.capture(), eq(true));
        CursorData nextCursor = CursorData.decode(cursorCaptor.getValue());
        assertThat(nextCursor.id()).isEqualTo(3L);
        assertThat(nextCursor.dateTimeValue()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("내 코스 목록은 완료한 코스 id 집합을 조회해 변환에 넘긴다")
    void getMyCourses_passesCompletedCourseIds() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 28, 12, 0);
        List<MyCourseView> views = List.of(myView(3L, createdAt), myView(7L, createdAt));
        given(courseRepository.findMyCourses(eq(1L), any(), any(), any(Pageable.class))).willReturn(views);
        given(courseRepository.findAvailableLines(1L)).willReturn(List.of());
        given(memberStampQueryService.getCompletedCourseIds(1L, List.of(3L, 7L))).willReturn(Set.of(7L));

        // when
        courseQueryService.getMyCourses(1L, null, null, null, 10);

        // then
        ArgumentCaptor<Set<Long>> completedCaptor = ArgumentCaptor.forClass(Set.class);
        verify(courseConverter).toMyListResponse(any(), completedCaptor.capture(), any(), any(), eq(false));
        assertThat(completedCaptor.getValue()).containsExactly(7L);
    }

    @Test
    @DisplayName("완료 여부는 페이지에 실린 코스만 한 번에 조회한다 (다음 페이지 확인용 초과분 제외)")
    void getMyCourses_completedLookupExcludesExtraRow() {
        // given: size 1 요청 → 2개 조회되지만 초과분은 응답에서 잘리므로 조회 대상도 아니다
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 28, 12, 0);
        List<MyCourseView> views = List.of(myView(3L, createdAt), myView(2L, createdAt));
        given(courseRepository.findMyCourses(eq(1L), any(), any(), any(Pageable.class))).willReturn(views);
        given(courseRepository.findAvailableLines(1L)).willReturn(List.of());

        // when
        courseQueryService.getMyCourses(1L, null, null, null, 1);

        // then
        verify(memberStampQueryService).getCompletedCourseIds(1L, List.of(3L));
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

    // ---------- 좋아요 여부 창구 ----------

    @Test
    @DisplayName("좋아요한 코스면 true를 반환한다")
    void isLikedByMember_liked() {
        // given
        given(courseLikeRepository.existsByMemberIdAndCourseId(1L, 10L)).willReturn(true);

        // when & then
        assertThat(courseQueryService.isLikedByMember(10L, 1L)).isTrue();
    }

    @Test
    @DisplayName("비로그인이면 조회하지 않고 false를 반환한다")
    void isLikedByMember_anonymous() {
        // when: 누를 사람이 없으므로 하트는 항상 비어 있다
        boolean result = courseQueryService.isLikedByMember(10L, null);

        // then
        assertThat(result).isFalse();
        verify(courseLikeRepository, never()).existsByMemberIdAndCourseId(any(), any());
    }
}
