package com.cotato.nextstation.domain.course.service;

import com.cotato.nextstation.domain.course.converter.CourseConverter;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private Course findOwnedCourse(Long memberId, Long courseId) {
        Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
        if (!course.getMemberId().equals(memberId)) {
            throw new CustomException(CourseErrorCode.COURSE_FORBIDDEN);
        }
        return course;
    }

    private void reorder(List<CoursePlace> coursePlaces, List<Long> orderedPlaceIds) {
        Map<Long, CoursePlace> byPlaceId = coursePlaces.stream()
                .collect(Collectors.toMap(CoursePlace::getPlaceId, Function.identity()));
        if (byPlaceId.size() != orderedPlaceIds.size()
                || !byPlaceId.keySet().equals(new HashSet<>(orderedPlaceIds))) {
            throw new CustomException(CourseErrorCode.INVALID_COURSE_PLACES);
        }
        for (int index = 0; index < orderedPlaceIds.size(); index++) {
            byPlaceId.get(orderedPlaceIds.get(index)).updateOrderNum(index + 1);
        }
    }
}
