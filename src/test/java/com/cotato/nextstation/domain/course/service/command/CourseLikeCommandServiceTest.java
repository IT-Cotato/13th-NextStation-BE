package com.cotato.nextstation.domain.course.service.command;

import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.course.entity.CourseLike;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.repository.CourseRepository;
import com.cotato.nextstation.domain.course.repository.CourseLikeRepository;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseLikeCommandServiceTest {

    @InjectMocks
    private CourseLikeCommandService courseLikeCommandService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseLikeRepository courseLikeRepository;

    private Course course(Long id, Long memberId) {
        Course course = Course.builder().memberId(memberId).stationId(10L).name("보문역 코스").build();
        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }

    @Test
    @DisplayName("코스를 좋아요하면 회원/코스가 정확히 저장되고 좋아요 수가 증가한다")
    void likeCourse_success() {
        // given: memberId와 courseId를 다른 값으로 둬야 둘이 뒤바뀌는 실수를 잡을 수 있다
        Long memberId = 1L;
        Long courseId = 7L;
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course(courseId, 2L)));
        given(courseRepository.existsPublicById(courseId)).willReturn(true);
        given(courseLikeRepository.existsByMemberIdAndCourseId(memberId, courseId)).willReturn(false);

        // when
        courseLikeCommandService.likeCourse(memberId, courseId);

        // then
        ArgumentCaptor<CourseLike> captor = ArgumentCaptor.forClass(CourseLike.class);
        verify(courseLikeRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(memberId);
        assertThat(captor.getValue().getCourseId()).isEqualTo(courseId);
        verify(courseRepository).increaseLikeCount(courseId);
    }

    @Test
    @DisplayName("이미 좋아요한 코스를 다시 좋아요하면 예외가 발생하고 좋아요 수가 늘지 않는다")
    void likeCourse_duplicate() {
        // given
        given(courseRepository.findById(1L)).willReturn(Optional.of(course(1L, 2L)));
        given(courseRepository.existsPublicById(1L)).willReturn(true);
        given(courseLikeRepository.existsByMemberIdAndCourseId(1L, 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> courseLikeCommandService.likeCourse(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.DUPLICATE_COURSE_LIKE.getMessage());
        verify(courseLikeRepository, never()).saveAndFlush(any());
        verify(courseRepository, never()).increaseLikeCount(any());
    }

    @Test
    @DisplayName("동시 요청으로 유니크 제약에 걸리면 중복 저장 예외로 응답한다")
    void likeCourse_raceCondition() {
        // given: 중복 확인은 통과했지만 저장 시점에 다른 요청이 먼저 커밋된 상황
        given(courseRepository.findById(1L)).willReturn(Optional.of(course(1L, 2L)));
        given(courseRepository.existsPublicById(1L)).willReturn(true);
        given(courseLikeRepository.existsByMemberIdAndCourseId(1L, 1L)).willReturn(false);
        willThrow(new DataIntegrityViolationException("unique"))
                .given(courseLikeRepository).saveAndFlush(any(CourseLike.class));

        // when & then
        assertThatThrownBy(() -> courseLikeCommandService.likeCourse(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.DUPLICATE_COURSE_LIKE.getMessage());
        verify(courseRepository, never()).increaseLikeCount(any());
    }

    @Test
    @DisplayName("존재하지 않는 코스는 좋아요할 수 없다")
    void likeCourse_courseNotFound() {
        // given
        given(courseRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> courseLikeCommandService.likeCourse(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_NOT_FOUND.getMessage());
        verify(courseLikeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("본인이 만든 코스는 좋아요할 수 없다")
    void likeCourse_ownCourse() {
        // given: 1번 회원이 자기 코스(memberId=1)를 좋아요 시도
        given(courseRepository.findById(1L)).willReturn(Optional.of(course(1L, 1L)));

        // when & then
        assertThatThrownBy(() -> courseLikeCommandService.likeCourse(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.CANNOT_LIKE_OWN_COURSE.getMessage());
        verify(courseLikeRepository, never()).saveAndFlush(any());
        verify(courseRepository, never()).increaseLikeCount(any());
    }

    @Test
    @DisplayName("공개되지 않은 코스는 좋아요할 수 없다")
    void likeCourse_notPublic() {
        // given: 타인 코스지만 일지가 없거나 비공개인 경우
        given(courseRepository.findById(1L)).willReturn(Optional.of(course(1L, 2L)));
        given(courseRepository.existsPublicById(1L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> courseLikeCommandService.likeCourse(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_NOT_FOUND.getMessage());
        verify(courseLikeRepository, never()).saveAndFlush(any());
        verify(courseRepository, never()).increaseLikeCount(any());
    }

    @Test
    @DisplayName("좋아요를 취소하면 삭제되고 좋아요 수가 감소한다")
    void cancelLike_success() {
        // given: 삭제 쿼리가 1행을 지웠다 = 실제로 좋아요돼 있었다
        given(courseLikeRepository.deleteByMemberIdAndCourseId(1L, 1L)).willReturn(1);

        // when
        courseLikeCommandService.cancelLike(1L, 1L);

        // then
        verify(courseRepository).decreaseLikeCount(1L);
    }

    @Test
    @DisplayName("좋아요하지 않은 코스를 취소하면 예외가 발생하고 좋아요 수가 줄지 않는다")
    void cancelLike_notSaved() {
        // given: 지워진 행이 없다 = 좋아요돼 있지 않았다
        given(courseLikeRepository.deleteByMemberIdAndCourseId(1L, 1L)).willReturn(0);

        // when & then
        assertThatThrownBy(() -> courseLikeCommandService.cancelLike(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_LIKE_NOT_FOUND.getMessage());
        verify(courseRepository, never()).decreaseLikeCount(any());
    }

    @Test
    @DisplayName("동시 취소로 이미 지워진 뒤라면 좋아요 수를 줄이지 않는다")
    void cancelLike_alreadyDeletedByConcurrentRequest() {
        // given: 조회 시점엔 있었더라도 삭제 시점에 다른 요청이 먼저 지웠다면 0행이 반환된다
        given(courseLikeRepository.deleteByMemberIdAndCourseId(1L, 1L)).willReturn(0);

        // when & then: 좋아요 수가 중복으로 깎이지 않는다
        assertThatThrownBy(() -> courseLikeCommandService.cancelLike(1L, 1L))
                .isInstanceOf(CustomException.class);
        verify(courseRepository, never()).decreaseLikeCount(any());
    }

    @Test
    @DisplayName("여러 좋아요를 취소하면 좋아요 수를 먼저 줄이고 한 번에 삭제한다")
    void cancelLikes_success() {
        // given: 1,2,3 중 2는 이미 취소돼 있어 좋아요 수는 2개만 줄어든다
        given(courseRepository.decreaseLikeCountAll(1L, List.of(1L, 2L, 3L))).willReturn(2);

        // when
        courseLikeCommandService.cancelLikes(1L, List.of(1L, 2L, 3L));

        // then: 삭제는 벌크로 한 번만 나간다
        verify(courseLikeRepository).deleteByMemberIdAndCourseIdIn(1L, List.of(1L, 2L, 3L));
    }

    @Test
    @DisplayName("좋아요 수 감소가 삭제보다 먼저 실행된다")
    void cancelLikes_decreaseBeforeDelete() {
        // given: 삭제 후에는 좋아요이 남아 있는지 알 수 없어 순서가 뒤바뀌면 좋아요 수가 과다 감소한다
        given(courseRepository.decreaseLikeCountAll(1L, List.of(1L))).willReturn(1);

        // when
        courseLikeCommandService.cancelLikes(1L, List.of(1L));

        // then
        InOrder inOrder = inOrder(courseRepository, courseLikeRepository);
        inOrder.verify(courseRepository).decreaseLikeCountAll(1L, List.of(1L));
        inOrder.verify(courseLikeRepository).deleteByMemberIdAndCourseIdIn(1L, List.of(1L));
    }

    @Test
    @DisplayName("같은 코스를 중복으로 보내도 한 번만 처리한다")
    void cancelLikes_duplicateIds() {
        // given
        given(courseRepository.decreaseLikeCountAll(1L, List.of(1L))).willReturn(1);

        // when
        courseLikeCommandService.cancelLikes(1L, List.of(1L, 1L, 1L));

        // then
        verify(courseLikeRepository).deleteByMemberIdAndCourseIdIn(1L, List.of(1L));
    }

    @Test
    @DisplayName("전체 취소는 화면에 안 불러온 좋아요까지 서버가 조회해 취소한다")
    void cancelAllLikes_success() {
        // given: 프론트가 첫 페이지 2개만 들고 있어도 서버는 5개 전부를 대상으로 삼는다
        given(courseLikeRepository.findVisibleLikedCourseIds(1L)).willReturn(List.of(1L, 2L, 3L, 4L, 5L));
        given(courseRepository.decreaseLikeCountAll(any(), any())).willReturn(5);

        // when
        courseLikeCommandService.cancelAllLikes(1L, null);

        // then
        verify(courseLikeRepository).deleteByMemberIdAndCourseIdIn(1L, List.of(1L, 2L, 3L, 4L, 5L));
    }

    @Test
    @DisplayName("전체 선택 후 해제한 코스는 취소 대상에서 빠진다")
    void cancelAllLikes_withExceptions() {
        // given: 전체 선택 뒤 2,4번을 해제한 경우
        given(courseLikeRepository.findVisibleLikedCourseIds(1L)).willReturn(List.of(1L, 2L, 3L, 4L, 5L));
        given(courseRepository.decreaseLikeCountAll(any(), any())).willReturn(3);

        // when
        courseLikeCommandService.cancelAllLikes(1L, List.of(2L, 4L));

        // then
        verify(courseRepository).decreaseLikeCountAll(1L, List.of(1L, 3L, 5L));
        verify(courseLikeRepository).deleteByMemberIdAndCourseIdIn(1L, List.of(1L, 3L, 5L));
    }

    @Test
    @DisplayName("취소할 좋아요이 없으면 예외가 발생한다")
    void cancelAllLikes_nothingToCancel() {
        // given
        given(courseLikeRepository.findVisibleLikedCourseIds(1L)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> courseLikeCommandService.cancelAllLikes(1L, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_LIKE_NOT_FOUND.getMessage());
        verify(courseLikeRepository, never()).deleteByMemberIdAndCourseIdIn(any(), any());
    }

    @Test
    @DisplayName("전체 선택 후 모두 해제하면 예외가 발생한다")
    void cancelAllLikes_allExcluded() {
        // given
        given(courseLikeRepository.findVisibleLikedCourseIds(1L)).willReturn(List.of(1L, 2L));

        // when & then
        assertThatThrownBy(() -> courseLikeCommandService.cancelAllLikes(1L, List.of(1L, 2L)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_LIKE_NOT_FOUND.getMessage());
        verify(courseLikeRepository, never()).deleteByMemberIdAndCourseIdIn(any(), any());
    }

    @Test
    @DisplayName("선택한 코스가 하나도 좋아요돼 있지 않으면 삭제하지 않고 예외가 발생한다")
    void cancelLikes_noneSaved() {
        // given: 좋아요 수가 하나도 안 줄었다 = 남아 있는 좋아요이 없었다
        given(courseRepository.decreaseLikeCountAll(any(), any())).willReturn(0);

        // when & then
        assertThatThrownBy(() -> courseLikeCommandService.cancelLikes(1L, List.of(1L, 2L)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_LIKE_NOT_FOUND.getMessage());
        verify(courseLikeRepository, never()).deleteByMemberIdAndCourseIdIn(any(), any());
    }

    @Test
    @DisplayName("본인 코스를 삭제하면 soft delete 된다")
    void deleteCourse_success() {
        // given
        Course course = course(1L, 1L);
        given(courseRepository.findById(1L)).willReturn(Optional.of(course));

        // when
        courseLikeCommandService.deleteCourse(1L, 1L);

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
        assertThatThrownBy(() -> courseLikeCommandService.deleteCourse(1L, 1L))
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
        assertThatThrownBy(() -> courseLikeCommandService.deleteCourse(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CourseErrorCode.COURSE_NOT_FOUND.getMessage());
    }
}
