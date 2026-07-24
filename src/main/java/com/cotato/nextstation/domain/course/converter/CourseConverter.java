package com.cotato.nextstation.domain.course.converter;

import com.cotato.nextstation.domain.course.dto.request.CourseCreateRequest;
import com.cotato.nextstation.domain.course.dto.response.CourseCreateResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseNameResponse;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.PopularCourseResponse;
import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.course.entity.CoursePlace;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Component
public class CourseConverter {

    public Course toCourse(Long memberId, CourseCreateRequest request) {
        return Course.builder()
                .memberId(memberId)
                .stationId(request.stationId())
                .name(request.name())
                .build();
    }

    // "내 코스로 만들기". 원본은 originalCourseId로만 기록하고, 이후 원본과 무관한 독립 코스로 존재한다.
    // 여행일지·컨셉투어와 조회수·저장수는 원본에서 물려받지 않고 새 코스 기준으로 시작한다.
    public Course toCopiedCourse(Long memberId, Course original, String name) {
        return Course.builder()
                .memberId(memberId)
                .stationId(original.getStationId())
                .name(name)
                .originalCourseId(original.getId())
                .build();
    }

    public List<CoursePlace> toCoursePlaces(Long courseId, List<Long> placeIds) {
        return IntStream.range(0, placeIds.size())
                .mapToObj(index -> CoursePlace.builder()
                        .courseId(courseId)
                        .placeId(placeIds.get(index))
                        .orderNum(index + 1)
                        .build())
                .toList();
    }

    public CourseCreateResponse toCreateResponse(Course course) {
        return new CourseCreateResponse(course.getId(), course.getName(), course.getCreatedAt());
    }

    public CourseNameResponse toNameResponse(Course course) {
        return new CourseNameResponse(course.getId(), course.getName());
    }

    public CourseInfoResponse toInfoResponse(Course course) {
        return new CourseInfoResponse(
                course.getId(),
                course.getName(),
                course.getMemberId(),
                course.getStationId(),
                course.getJournalId(),
                course.getViewCount(),
                course.getSaveCount(),
                course.getCreatedAt()
        );
    }

    public List<CoursePlaceInfoResponse> toPlaceInfoResponses(List<CoursePlace> coursePlaces) {
        return coursePlaces.stream()
                .map(coursePlace -> new CoursePlaceInfoResponse(coursePlace.getPlaceId(), coursePlace.getOrderNum()))
                .toList();
    }

    public List<PopularCourseResponse> toPopularResponses(List<Course> courses, Set<Long> savedCourseIds) {
        return courses.stream()
                .map(course -> new PopularCourseResponse(
                        course.getId(),
                        course.getName(),
                        course.getViewCount(),
                        course.getSaveCount(),
                        savedCourseIds.contains(course.getId())
                ))
                .toList();
    }
}
