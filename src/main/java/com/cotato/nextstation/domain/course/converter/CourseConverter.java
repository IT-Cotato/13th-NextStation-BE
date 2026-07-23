package com.cotato.nextstation.domain.course.converter;

import com.cotato.nextstation.domain.course.dto.request.CourseCreateRequest;
import com.cotato.nextstation.domain.course.dto.response.CourseCardResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseCreateResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseNameResponse;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.LineFilterResponse;
import com.cotato.nextstation.domain.course.dto.response.MyCourseListResponse;
import com.cotato.nextstation.domain.course.dto.response.PopularCourseResponse;
import com.cotato.nextstation.domain.course.dto.response.SavedCourseListResponse;
import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.course.entity.CoursePlace;
import com.cotato.nextstation.domain.course.repository.CourseRepository.LineView;
import com.cotato.nextstation.domain.course.repository.CourseRepository.MyCourseView;
import com.cotato.nextstation.domain.course.repository.CourseSaveRepository.SavedCourseView;
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

    public SavedCourseListResponse toSavedListResponse(List<SavedCourseView> savedCourses,
                                                       String nextCursor, boolean hasNext) {
        List<CourseCardResponse> cards = savedCourses.stream()
                .map(saved -> new CourseCardResponse(
                        saved.getCourseId(),
                        saved.getName(),
                        saved.getStationId(),
                        saved.getStationName(),
                        saved.getLineId(),
                        saved.getLineName()))
                .toList();
        return new SavedCourseListResponse(cards, nextCursor, hasNext);
    }

    public MyCourseListResponse toMyListResponse(List<MyCourseView> myCourses, List<LineView> availableLines,
                                                 String nextCursor, boolean hasNext) {
        List<CourseCardResponse> cards = myCourses.stream()
                .map(myCourse -> new CourseCardResponse(
                        myCourse.getCourseId(),
                        myCourse.getName(),
                        myCourse.getStationId(),
                        myCourse.getStationName(),
                        myCourse.getLineId(),
                        myCourse.getLineName()))
                .toList();
        List<LineFilterResponse> lineFilters = availableLines.stream()
                .map(line -> new LineFilterResponse(line.getLineId(), line.getLineName()))
                .toList();
        return new MyCourseListResponse(lineFilters, cards, nextCursor, hasNext);
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
