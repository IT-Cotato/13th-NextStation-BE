package com.cotato.nextstation.domain.course.service.query;

import com.cotato.nextstation.domain.course.converter.CourseConverter;
import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceInfoResponse;
import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.repository.CoursePlaceRepository;
import com.cotato.nextstation.domain.course.repository.CourseRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 다른 도메인(Part3 등)이 코스를 조회할 때 사용하는 전용 포트.
// CourseRepository/CourseErrorCode에 직접 의존하지 않고 이 서비스만 호출하도록 한다 (PR #23 원칙).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryService {

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final CourseConverter courseConverter;

    public CourseInfoResponse getCourseInfo(Long courseId) {
        return courseConverter.toInfoResponse(findCourse(courseId));
    }

    public List<CoursePlaceInfoResponse> getCoursePlaces(Long courseId) {
        findCourse(courseId);
        return courseConverter.toPlaceInfoResponses(coursePlaceRepository.findByCourseIdOrderByOrderNumAsc(courseId));
    }

    private Course findCourse(Long courseId) {
        // 삭제된 코스는 Course의 @SQLRestriction 으로 조회에서 자동 제외된다.
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
    }
}
