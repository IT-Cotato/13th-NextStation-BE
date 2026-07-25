package com.cotato.nextstation.domain.course.service.command;

import com.cotato.nextstation.domain.course.converter.CourseConverter;
import com.cotato.nextstation.domain.course.dto.request.CourseCopyRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseCreateRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseNameUpdateRequest;
import com.cotato.nextstation.domain.course.dto.request.CoursePlaceOrderUpdateRequest;
import com.cotato.nextstation.domain.course.dto.response.CourseCreateResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseNameResponse;
import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.course.entity.CoursePlace;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.repository.CoursePlaceRepository;
import com.cotato.nextstation.domain.course.repository.CourseRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourseCommandService {

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final CourseConverter courseConverter;

    public CourseCreateResponse createCourse(Long memberId, CourseCreateRequest request) {
        validateDistinctPlaces(request.placeIds());

        Course savedCourse = courseRepository.save(courseConverter.toCourse(memberId, request));
        coursePlaceRepository.saveAll(courseConverter.toCoursePlaces(savedCourse.getId(), request.placeIds()));
        return courseConverter.toCreateResponse(savedCourse);
    }

    /**
     * "내 코스로 만들기"
     * 타인의 공개 코스를 편집 가능한 내 코스로 복제한다.
     * 원본을 참조만 하는 좋아요와 달리, 복제본은 원본이 삭제되거나 비공개로 바뀌어도 그대로 남는다.
     * 저장 화면에서 이름 입력과 순서 변경이 함께 일어나므로 한 번의 요청으로 처리한다.
     */
    public CourseCreateResponse copyCourse(Long memberId, Long courseId, CourseCopyRequest request) {
        Course original = findCopyableCourse(memberId, courseId);
        List<Long> placeIds = resolveCopiedPlaceOrder(courseId, request.placeIds());

        Course copied = courseRepository.save(courseConverter.toCopiedCourse(memberId, original, request.name()));
        coursePlaceRepository.saveAll(courseConverter.toCoursePlaces(copied.getId(), placeIds));
        return courseConverter.toCreateResponse(copied);
    }

    public CourseNameResponse updateCourseName(Long memberId, Long courseId, CourseNameUpdateRequest request) {
        Course course = findOwnedCourse(memberId, courseId);
        course.updateName(request.name());
        return courseConverter.toNameResponse(course);
    }

    public void updateCoursePlaceOrder(Long memberId, Long courseId, CoursePlaceOrderUpdateRequest request) {
        validateDistinctPlaces(request.placeIds());

        findOwnedCourse(memberId, courseId);
        reorder(coursePlaceRepository.findByCourseIdOrderByOrderNumAsc(courseId), request.placeIds());
    }

    // 같은 장소를 한 코스에 두 번 담을 수 없다.
    private void validateDistinctPlaces(List<Long> placeIds) {
        if (new HashSet<>(placeIds).size() != placeIds.size()) {
            throw new CustomException(CourseErrorCode.DUPLICATE_COURSE_PLACES);
        }
    }

    // 요청한 placeIds가 코스의 장소 구성과 정확히 일치하는지 검증한다 (개수·구성 모두).
    // 순서만 바꿀 수 있고 장소를 빼거나 더할 수는 없어서, 순서 수정과 복제가 같은 규칙을 공유한다.
    private void validateSamePlaceComposition(Collection<Long> coursePlaceIds, List<Long> requestedPlaceIds) {
        if (coursePlaceIds.size() != requestedPlaceIds.size()
                || !new HashSet<>(coursePlaceIds).equals(new HashSet<>(requestedPlaceIds))) {
            throw new CustomException(CourseErrorCode.INVALID_COURSE_PLACES);
        }
    }

    // 복제 대상은 "타인의 공개 코스"로 제한한다. 좋아요와 같은 조건이다.
    private Course findCopyableCourse(Long memberId, Long courseId) {
        // 삭제된 코스는 Course의 @SQLRestriction 으로 조회에서 자동 제외된다.
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));

        if (course.getMemberId().equals(memberId)) {
            throw new CustomException(CourseErrorCode.CANNOT_COPY_OWN_COURSE);
        }

        // 남의 비공개 코스가 존재한다는 사실이 드러나지 않도록 403이 아닌 404로 응답한다.
        if (!courseRepository.existsPublicById(courseId)) {
            log.warn("공개되지 않은 코스 복제 시도: memberId={}, courseId={}", memberId, courseId);
            throw new CustomException(CourseErrorCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    /**
     * 복제본에 적용할 장소 순서를 정한다.
     * 화면에서 순서 변경만 가능하고 장소를 빼거나 더할 수는 없으므로,
     * 요청한 목록은 원본의 장소 구성과 정확히 일치해야 한다. 생략하면 원본 순서를 그대로 쓴다.
     */
    private List<Long> resolveCopiedPlaceOrder(Long courseId, List<Long> requestedPlaceIds) {
        List<Long> originalPlaceIds = coursePlaceRepository.findByCourseIdOrderByOrderNumAsc(courseId).stream()
                .map(CoursePlace::getPlaceId)
                .toList();

        if (requestedPlaceIds == null || requestedPlaceIds.isEmpty()) {
            return originalPlaceIds;
        }

        validateDistinctPlaces(requestedPlaceIds);
        validateSamePlaceComposition(originalPlaceIds, requestedPlaceIds);
        return requestedPlaceIds;
    }

    private Course findOwnedCourse(Long memberId, Long courseId) {
        // 삭제된 코스는 Course의 @SQLRestriction 으로 조회에서 자동 제외된다.
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        if (!course.getMemberId().equals(memberId)) {
            throw new CustomException(CourseErrorCode.COURSE_FORBIDDEN);
        }
        return course;
    }

    private void reorder(List<CoursePlace> coursePlaces, List<Long> orderedPlaceIds) {
        Map<Long, CoursePlace> byPlaceId = coursePlaces.stream()
                .collect(Collectors.toMap(CoursePlace::getPlaceId, Function.identity()));
        validateSamePlaceComposition(byPlaceId.keySet(), orderedPlaceIds);
        for (int index = 0; index < orderedPlaceIds.size(); index++) {
            byPlaceId.get(orderedPlaceIds.get(index)).updateOrderNum(index + 1);
        }
    }
}
