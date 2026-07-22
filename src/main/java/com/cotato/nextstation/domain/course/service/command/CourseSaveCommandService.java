package com.cotato.nextstation.domain.course.service.command;

import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.course.entity.CourseSave;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.repository.CourseRepository;
import com.cotato.nextstation.domain.course.repository.CourseSaveRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourseSaveCommandService {

    private final CourseRepository courseRepository;
    private final CourseSaveRepository courseSaveRepository;

    // 코스 스크랩. 스크랩은 "타인의 공개 코스"만 대상으로 한다.
    public void saveCourse(Long memberId, Long courseId) {
        validateSavable(memberId, courseId);

        if (courseSaveRepository.existsByMemberIdAndCourseId(memberId, courseId)) {
            throw new CustomException(CourseErrorCode.DUPLICATE_COURSE_SAVE);
        }

        try {
            courseSaveRepository.saveAndFlush(
                    CourseSave.builder().memberId(memberId).courseId(courseId).build()
            );
        } catch (DataIntegrityViolationException e) {
            // 위 존재 확인 이후 동시에 같은 요청이 먼저 저장된 경우 (레이스 컨디션)
            log.warn("코스 중복 저장 시도(레이스 컨디션): memberId={}, courseId={}", memberId, courseId);
            throw new CustomException(CourseErrorCode.DUPLICATE_COURSE_SAVE);
        }

        courseRepository.increaseSaveCount(courseId);
    }

    // 코스 스크랩 취소. 저장하지 않은 코스면 404.
    // 조회 없이 바로 삭제하고 지워진 행 수로 판단한다. 조회 후 삭제하면 동시 취소 시
    // 두 요청이 모두 삭제에 성공했다고 보고 저장 수를 각각 줄인다.
    public void cancelSave(Long memberId, Long courseId) {
        if (courseSaveRepository.deleteByMemberIdAndCourseId(memberId, courseId) == 0) {
            throw new CustomException(CourseErrorCode.COURSE_SAVE_NOT_FOUND);
        }

        courseRepository.decreaseSaveCount(courseId);
    }

    /**
     * 스크랩 다중 취소(저장 탭의 선택 모드). 요청한 코스 중 실제로 저장돼 있는 것만 취소한다.
     * 다른 기기에서 이미 취소된 코스가 섞여 있어도 나머지는 정상 처리되도록 부분 성공을 허용하고,
     * 하나도 저장돼 있지 않을 때만 예외로 알린다.
     */
    public void cancelSaves(Long memberId, List<Long> courseIds) {
        // 단건과 같은 이유로, 미리 조회한 목록이 아니라 실제로 지워진 코스만 저장 수를 줄인다.
        List<Long> deletedCourseIds = courseIds.stream()
                .distinct()
                .filter(courseId -> courseSaveRepository.deleteByMemberIdAndCourseId(memberId, courseId) > 0)
                .toList();

        if (deletedCourseIds.isEmpty()) {
            throw new CustomException(CourseErrorCode.COURSE_SAVE_NOT_FOUND);
        }

        courseRepository.decreaseSaveCountAll(deletedCourseIds);
    }

    // 코스 삭제(soft delete). 본인 코스만 삭제할 수 있다.
    public void deleteCourse(Long memberId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));

        if (!course.getMemberId().equals(memberId)) {
            log.warn("타인 코스 삭제 시도: memberId={}, courseId={}", memberId, courseId);
            throw new CustomException(CourseErrorCode.COURSE_DELETE_FORBIDDEN);
        }

        course.delete();
    }

    private void validateSavable(Long memberId, Long courseId) {
        // 삭제된 코스는 Course의 @SQLRestriction으로 조회에서 자동 제외된다.
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));

        if (course.getMemberId().equals(memberId)) {
            throw new CustomException(CourseErrorCode.CANNOT_SAVE_OWN_COURSE);
        }

        // 공개되지 않은 코스는 스크랩해도 목록에 뜨지 않으므로 애초에 막는다.
        // 남의 비공개 코스가 존재한다는 사실이 드러나지 않도록 404로 응답한다.
        if (!courseRepository.existsPublicById(courseId)) {
            log.warn("공개되지 않은 코스 저장 시도: memberId={}, courseId={}", memberId, courseId);
            throw new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
        }
    }
}
