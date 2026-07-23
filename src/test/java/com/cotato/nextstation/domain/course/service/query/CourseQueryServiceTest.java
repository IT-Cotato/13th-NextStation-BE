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
import com.cotato.nextstation.domain.course.repository.CourseRepository.LineView;
import com.cotato.nextstation.domain.course.repository.CourseRepository.MyCourseView;
import com.cotato.nextstation.domain.course.repository.CourseSaveRepository;
import com.cotato.nextstation.domain.course.repository.CourseSaveRepository.SavedCourseView;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    // ---------- 저장 탭 - 스크랩한 코스 목록 ----------

    // 프로젝션은 인터페이스라 mock으로 만든다. 스터빙이 들어 있으므로 given(...) 밖에서 미리 생성한다.
    private SavedCourseView savedView(Long saveId, Long courseId, LocalDateTime savedAt) {
        SavedCourseView view = mock(SavedCourseView.class);
        lenient().when(view.getSaveId()).thenReturn(saveId);
        lenient().when(view.getCourseId()).thenReturn(courseId);
        lenient().when(view.getSavedAt()).thenReturn(savedAt);
        return view;
    }

    private MyCourseView myView(Long courseId, LocalDateTime createdAt) {
        MyCourseView view = mock(MyCourseView.class);
        lenient().when(view.getCourseId()).thenReturn(courseId);
        lenient().when(view.getCreatedAt()).thenReturn(createdAt);
        return view;
    }

    @Test
    @DisplayName("스크랩 목록은 요청 크기보다 1개 더 조회해 다음 페이지 여부를 판단하고, 초과분은 잘라낸다")
    void getSavedCourses_hasNext() {
        // given: size 2를 요청했는데 3개가 조회되면 다음 페이지가 있다는 뜻이다
        LocalDateTime savedAt = LocalDateTime.of(2026, 7, 23, 12, 0);
        List<SavedCourseView> views = List.of(
                savedView(30L, 3L, savedAt), savedView(20L, 2L, savedAt), savedView(10L, 1L, savedAt));
        given(courseSaveRepository.findSavedCourses(eq(1L), any(Pageable.class))).willReturn(views);

        // when
        courseQueryService.getSavedCourses(1L, null, 2);

        // then: 조회는 3개(=2+1), 응답에 담기는 건 2개
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(courseSaveRepository).findSavedCourses(eq(1L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 3));

        ArgumentCaptor<List<SavedCourseView>> contentCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> cursorCaptor = ArgumentCaptor.forClass(String.class);
        verify(courseConverter).toSavedListResponse(contentCaptor.capture(), cursorCaptor.capture(), eq(true));
        assertThat(contentCaptor.getValue()).hasSize(2);

        // 다음 커서는 마지막 항목 기준(스크랩 시각 + course_save.id)으로 만들어진다
        CursorData nextCursor = CursorData.decode(cursorCaptor.getValue());
        assertThat(nextCursor.id()).isEqualTo(20L);
        assertThat(nextCursor.dateTimeValue()).isEqualTo(savedAt);
        assertThat(nextCursor.longValue()).isNull();
    }

    @Test
    @DisplayName("마지막 페이지면 다음 커서 없이 응답한다")
    void getSavedCourses_lastPage() {
        // given: savedView가 내부에서 스터빙하므로 given(...) 안에서 호출하면 중첩 스터빙이 된다. 미리 만들어 둔다.
        SavedCourseView view = savedView(10L, 1L, LocalDateTime.of(2026, 7, 23, 12, 0));
        given(courseSaveRepository.findSavedCourses(eq(1L), any(Pageable.class))).willReturn(List.of(view));

        // when
        courseQueryService.getSavedCourses(1L, null, 2);

        // then
        verify(courseConverter).toSavedListResponse(any(), eq(null), eq(false));
    }

    @Test
    @DisplayName("커서를 주면 그 시각 이후 페이지를 조회한다")
    void getSavedCourses_withCursor() {
        // given
        LocalDateTime savedAt = LocalDateTime.of(2026, 7, 23, 12, 0);
        String cursor = new CursorData(20L, null, savedAt).encode();
        given(courseSaveRepository.findSavedCoursesAfterCursor(eq(1L), eq(savedAt), eq(20L), any(Pageable.class)))
                .willReturn(List.of());

        // when
        courseQueryService.getSavedCourses(1L, cursor, 2);

        // then: 첫 페이지 쿼리는 타지 않는다
        verify(courseSaveRepository, never()).findSavedCourses(any(), any());
        verify(courseSaveRepository).findSavedCoursesAfterCursor(eq(1L), eq(savedAt), eq(20L), any(Pageable.class));
    }

    @Test
    @DisplayName("정렬 기준과 맞지 않는 커서면 예외가 발생한다")
    void getSavedCourses_invalidCursor() {
        // given: 시간순 목록인데 숫자 커서(인기순용)가 들어온 경우
        String cursor = new CursorData(20L, 100L, null).encode();

        // when & then
        assertThatThrownBy(() -> courseQueryService.getSavedCourses(1L, cursor, 2))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(GlobalErrorCode.INVALID_CURSOR.getMessage());
    }

    @Test
    @DisplayName("size가 허용 범위를 벗어나면 예외가 발생한다")
    void getSavedCourses_invalidSize() {
        assertThatThrownBy(() -> courseQueryService.getSavedCourses(1L, null, 0))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(GlobalErrorCode.INVALID_PAGE_SIZE.getMessage());
        assertThatThrownBy(() -> courseQueryService.getSavedCourses(1L, null, 51))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(GlobalErrorCode.INVALID_PAGE_SIZE.getMessage());
    }

    @Test
    @DisplayName("size를 생략하면 기본 크기로 조회한다")
    void getSavedCourses_defaultSize() {
        // given
        given(courseSaveRepository.findSavedCourses(eq(1L), any(Pageable.class))).willReturn(List.of());

        // when
        courseQueryService.getSavedCourses(1L, null, null);

        // then: 기본 10 + hasNext 판단용 1
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(courseSaveRepository).findSavedCourses(eq(1L), pageableCaptor.capture());
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
        verify(courseConverter).toMyListResponse(any(), linesCaptor.capture(), any(), eq(false));
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
        verify(courseConverter).toMyListResponse(any(), any(), cursorCaptor.capture(), eq(true));
        CursorData nextCursor = CursorData.decode(cursorCaptor.getValue());
        assertThat(nextCursor.id()).isEqualTo(3L);
        assertThat(nextCursor.dateTimeValue()).isEqualTo(createdAt);
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
