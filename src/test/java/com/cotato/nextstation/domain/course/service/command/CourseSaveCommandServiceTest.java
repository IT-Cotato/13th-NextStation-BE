package com.cotato.nextstation.domain.course.service.command;

import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.course.entity.CourseSave;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.repository.CourseRepository;
import com.cotato.nextstation.domain.course.repository.CourseSaveRepository;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseSaveCommandServiceTest {

    @InjectMocks
    private CourseSaveCommandService courseSaveCommandService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseSaveRepository courseSaveRepository;

    private Course course(Long id, Long memberId) {
        Course course = Course.builder().memberId(memberId).stationId(10L).name("보문역 코스").build();
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    @Test
    @DisplayName("코스를 스크랩하면 회원/코스가 정확히 저장되고 저장 수가 증가한다")
    void saveCourse_success() {
        // given: memberId와 courseId를 다른 값으로 둬야 둘이 뒤바뀌는 실수를 잡을 수 있다
        Long memberId = 1L;
        Long courseId = 7L;
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course(courseId, 2L)));
        given(courseRepository.existsPublicById(courseId)).willReturn(true);
        given(courseSaveRepository.existsByMemberIdAndCourseId(memberId, courseId)).willReturn(false);

        // when
        courseSaveCommandService.saveCourse(memberId, courseId);

        // then
        ArgumentCaptor<CourseSave> captor = ArgumentCaptor.forClass(CourseSave.class);
        verify(courseSaveRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(memberId);
        assertThat(captor.getValue().getCourseId()).isEqualTo(courseId);
        verify(courseRepository).increaseSaveCount(courseId);
    }

    @Test
    @DisplayName("이미 저장한 코스를 다시 스크랩하면 예외가 발생하고 저장 수가 늘지 않는다")
    void saveCourse_duplicate() {
        // given
        given(courseRepository.findById(1L)).willReturn(Optional.of(course(1L, 2L)));
        given(courseRepository.existsPublicById(1L)).willReturn(true);
        given(courseSaveRepository.existsByMemberIdAndCourseId(1L, 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> courseSaveCommandService.saveCourse(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.DUPLICATE_COURSE_SAVE.getMessage());
        verify(courseSaveRepository, never()).saveAndFlush(any());
        verify(courseRepository, never()).increaseSaveCount(any());
    }

    @Test
    @DisplayName("동시 요청으로 유니크 제약에 걸리면 중복 저장 예외로 응답한다")
    void saveCourse_raceCondition() {
        // given: 중복 확인은 통과했지만 저장 시점에 다른 요청이 먼저 커밋된 상황
        given(courseRepository.findById(1L)).willReturn(Optional.of(course(1L, 2L)));
        given(courseRepository.existsPublicById(1L)).willReturn(true);
        given(courseSaveRepository.existsByMemberIdAndCourseId(1L, 1L)).willReturn(false);
        willThrow(new DataIntegrityViolationException("unique"))
                .given(courseSaveRepository).saveAndFlush(any(CourseSave.class));

        // when & then
        assertThatThrownBy(() -> courseSaveCommandService.saveCourse(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.DUPLICATE_COURSE_SAVE.getMessage());
        verify(courseRepository, never()).increaseSaveCount(any());
    }

    @Test
    @DisplayName("존재하지 않는 코스는 스크랩할 수 없다")
    void saveCourse_courseNotFound() {
        // given
        given(courseRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> courseSaveCommandService.saveCourse(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_NOT_FOUND.getMessage());
        verify(courseSaveRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("본인이 만든 코스는 스크랩할 수 없다")
    void saveCourse_ownCourse() {
        // given: 1번 회원이 자기 코스(memberId=1)를 스크랩 시도
        given(courseRepository.findById(1L)).willReturn(Optional.of(course(1L, 1L)));

        // when & then
        assertThatThrownBy(() -> courseSaveCommandService.saveCourse(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.CANNOT_SAVE_OWN_COURSE.getMessage());
        verify(courseSaveRepository, never()).saveAndFlush(any());
        verify(courseRepository, never()).increaseSaveCount(any());
    }

    @Test
    @DisplayName("공개되지 않은 코스는 스크랩할 수 없다")
    void saveCourse_notPublic() {
        // given: 타인 코스지만 일지가 없거나 비공개인 경우
        given(courseRepository.findById(1L)).willReturn(Optional.of(course(1L, 2L)));
        given(courseRepository.existsPublicById(1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> courseSaveCommandService.saveCourse(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_NOT_FOUND.getMessage());
        verify(courseSaveRepository, never()).saveAndFlush(any());
        verify(courseRepository, never()).increaseSaveCount(any());
    }

    @Test
    @DisplayName("스크랩을 취소하면 삭제되고 저장 수가 감소한다")
    void cancelSave_success() {
        // given: 삭제 쿼리가 1행을 지웠다 = 실제로 스크랩돼 있었다
        given(courseSaveRepository.deleteByMemberIdAndCourseId(1L, 1L)).willReturn(1);

        // when
        courseSaveCommandService.cancelSave(1L, 1L);

        // then
        verify(courseRepository).decreaseSaveCount(1L);
    }

    @Test
    @DisplayName("저장하지 않은 코스를 취소하면 예외가 발생하고 저장 수가 줄지 않는다")
    void cancelSave_notSaved() {
        // given: 지워진 행이 없다 = 저장돼 있지 않았다
        given(courseSaveRepository.deleteByMemberIdAndCourseId(1L, 1L)).willReturn(0);

        // when & then
        assertThatThrownBy(() -> courseSaveCommandService.cancelSave(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_SAVE_NOT_FOUND.getMessage());
        verify(courseRepository, never()).decreaseSaveCount(any());
    }

    @Test
    @DisplayName("동시 취소로 이미 지워진 뒤라면 저장 수를 줄이지 않는다")
    void cancelSave_alreadyDeletedByConcurrentRequest() {
        // given: 조회 시점엔 있었더라도 삭제 시점에 다른 요청이 먼저 지웠다면 0행이 반환된다
        given(courseSaveRepository.deleteByMemberIdAndCourseId(1L, 1L)).willReturn(0);

        // when & then: 저장 수가 중복으로 깎이지 않는다
        assertThatThrownBy(() -> courseSaveCommandService.cancelSave(1L, 1L))
                .isInstanceOf(CustomException.class);
        verify(courseRepository, never()).decreaseSaveCount(any());
    }

    @Test
    @DisplayName("여러 스크랩을 한 번에 취소하면 실제로 지워진 것만 저장 수가 줄어든다")
    void cancelSaves_success() {
        // given: 1,2,3을 요청했지만 2는 이미 취소돼 있어 0행이 지워진다
        given(courseSaveRepository.deleteByMemberIdAndCourseId(1L, 1L)).willReturn(1);
        given(courseSaveRepository.deleteByMemberIdAndCourseId(1L, 2L)).willReturn(0);
        given(courseSaveRepository.deleteByMemberIdAndCourseId(1L, 3L)).willReturn(1);

        // when
        courseSaveCommandService.cancelSaves(1L, List.of(1L, 2L, 3L));

        // then: 저장돼 있지 않던 2번은 빠지고 1,3번만 감소한다
        verify(courseRepository).decreaseSaveCountAll(List.of(1L, 3L));
    }

    @Test
    @DisplayName("같은 코스를 중복으로 보내도 한 번만 처리한다")
    void cancelSaves_duplicateIds() {
        // given
        given(courseSaveRepository.deleteByMemberIdAndCourseId(1L, 1L)).willReturn(1);

        // when
        courseSaveCommandService.cancelSaves(1L, List.of(1L, 1L, 1L));

        // then: 삭제도 감소도 한 번씩만
        verify(courseSaveRepository).deleteByMemberIdAndCourseId(1L, 1L);
        verify(courseRepository).decreaseSaveCountAll(List.of(1L));
    }

    @Test
    @DisplayName("선택한 코스가 하나도 저장돼 있지 않으면 예외가 발생한다")
    void cancelSaves_noneSaved() {
        // given
        given(courseSaveRepository.deleteByMemberIdAndCourseId(any(), any())).willReturn(0);

        // when & then
        assertThatThrownBy(() -> courseSaveCommandService.cancelSaves(1L, List.of(1L, 2L)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_SAVE_NOT_FOUND.getMessage());
        verify(courseRepository, never()).decreaseSaveCountAll(any());
    }

    @Test
    @DisplayName("본인 코스를 삭제하면 soft delete 된다")
    void deleteCourse_success() {
        // given
        Course course = course(1L, 1L);
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));

        // when
        courseSaveCommandService.deleteCourse(1L, 1L);

        // then
        assertThat(course.isDeleted()).isTrue();
        assertThat(course.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("타인 코스를 삭제하면 예외가 발생하고 삭제되지 않는다")
    void deleteCourse_forbidden() {
        // given: 코스 주인은 2번 회원인데 1번 회원이 삭제 시도
        Course course = course(1L, 2L);
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));

        // when & then
        assertThatThrownBy(() -> courseSaveCommandService.deleteCourse(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_DELETE_FORBIDDEN.getMessage());
        assertThat(course.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 코스를 삭제하면 예외가 발생한다")
    void deleteCourse_notFound() {
        // given
        given(courseRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> courseSaveCommandService.deleteCourse(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_NOT_FOUND.getMessage());
    }
}
