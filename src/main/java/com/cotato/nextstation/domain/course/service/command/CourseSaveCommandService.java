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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourseSaveCommandService {

    private final CourseRepository courseRepository;
    private final CourseSaveRepository courseSaveRepository;

    // 코스 스크랩. 이미 저장한 코스면 409.
    public void saveCourse(Long memberId, Long courseId) {
        validateCourseExists(courseId);

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
    public void cancelSave(Long memberId, Long courseId) {
        CourseSave courseSave = courseSaveRepository.findByMemberIdAndCourseId(memberId, courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_SAVE_NOT_FOUND));

        courseSaveRepository.delete(courseSave);
        courseRepository.decreaseSaveCount(courseId);
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

    private void validateCourseExists(Long courseId) {
        // 삭제된 코스는 Course의 @SQLRestriction으로 조회에서 자동 제외된다.
        if (!courseRepository.existsById(courseId)) {
            throw new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
        }
    }
}
