package com.cotato.nextstation.domain.course.converter;

import com.cotato.nextstation.domain.course.dto.request.CourseCreateRequest;
import com.cotato.nextstation.domain.course.dto.response.CourseCardResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseCreateResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.CourseUpdateResponse;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.MyCourseCardResponse;
import com.cotato.nextstation.domain.course.dto.response.MyCourseDetailResponse;
import com.cotato.nextstation.domain.course.dto.response.MyCourseListResponse;
import com.cotato.nextstation.domain.course.dto.response.MyCoursePlaceResponse;
import com.cotato.nextstation.domain.course.dto.response.PlaceCourseResponse;
import com.cotato.nextstation.domain.course.dto.response.PopularCourseResponse;
import com.cotato.nextstation.domain.course.dto.response.LikedCourseListResponse;
import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.course.entity.CoursePlace;
import com.cotato.nextstation.domain.course.repository.CourseRepository.LineView;
import com.cotato.nextstation.domain.course.repository.CourseRepository.MyCourseDetailView;
import com.cotato.nextstation.domain.course.repository.CourseRepository.MyCourseView;
import com.cotato.nextstation.domain.course.repository.CourseRepository.PlaceCourseView;
import com.cotato.nextstation.domain.course.repository.CourseLikeRepository.LikedCourseView;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.LineCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Component
public class CourseConverter {

    // 소요시간 구간. 여행기록 작성 화면의 "코스 시간" 선택지(3~4시간/반나절/하루종일)와 같은 값을 쓴다.
    // 추후 여행일지의 travel_duration으로 갈아끼울 때 값이 그대로 대응되도록 맞춰 둔 것이다.
    private static final String DURATION_SHORT = "SHORT";
    private static final String DURATION_HALF_DAY = "HALF_DAY";
    private static final String DURATION_FULL_DAY = "FULL_DAY";

    // 장소 수 → 구간 경계. 기획 확인 후 조정될 수 있어 분리해 뒀다.
    private static final int HALF_DAY_MIN_PLACES = 5;
    private static final int FULL_DAY_MIN_PLACES = 8;

    public Course toCourse(Long memberId, CourseCreateRequest request) {
        return Course.builder()
                .memberId(memberId)
                .stationId(request.stationId())
                .name(request.name())
                .build();
    }

    // "내 코스로 만들기" 원본은 originalCourseId로만 기록하고, 이후 원본과 무관한 독립 코스로 존재한다.
    // 여행일지·컨셉투어와 조회수·좋아요수는 원본에서 물려받지 않고 새 코스 기준으로 시작한다.
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

    public CourseUpdateResponse toUpdateResponse(Course course) {
        return new CourseUpdateResponse(course.getId(), course.getName());
    }

    public CourseInfoResponse toInfoResponse(Course course) {
        return new CourseInfoResponse(
                course.getId(),
                course.getName(),
                course.getMemberId(),
                course.getStationId(),
                course.getJournalId(),
                course.getViewCount(),
                course.getLikeCount(),
                course.getCreatedAt()
        );
    }

    public MyCourseDetailResponse toMyCourseDetailResponse(MyCourseDetailView course, List<MyCoursePlaceResponse> places) {
        return new MyCourseDetailResponse(
                course.getCourseId(),
                course.getName(),
                course.getStationId(),
                course.getStationName(),
                toLine(course.getLineId(), course.getLineName(), course.getLineCode()),
                places
        );
    }

    public MyCoursePlaceResponse toMyCoursePlaceResponse(PlaceInfoResponse place, int orderNum) {
        return new MyCoursePlaceResponse(
                place.placeId(),
                place.placeName(),
                place.description(),
                place.imageUrl(),
                place.xCoordinate(),
                place.yCoordinate(),
                orderNum
        );
    }

    public List<CoursePlaceInfoResponse> toPlaceInfoResponses(List<CoursePlace> coursePlaces) {
        return coursePlaces.stream()
                .map(coursePlace -> new CoursePlaceInfoResponse(coursePlace.getPlaceId(), coursePlace.getOrderNum()))
                .toList();
    }

    public LikedCourseListResponse toLikedListResponse(List<LikedCourseView> likedCourses,
                                                       String nextCursor, boolean hasNext) {
        List<CourseCardResponse> cards = likedCourses.stream()
                .map(liked -> new CourseCardResponse(
                        liked.getCourseId(),
                        liked.getName(),
                        liked.getStationId(),
                        liked.getStationName(),
                        toLine(liked.getLineId(), liked.getLineName(), liked.getLineCode())))
                .toList();
        return new LikedCourseListResponse(cards, nextCursor, hasNext);
    }

    public MyCourseListResponse toMyListResponse(List<MyCourseView> myCourses, Set<Long> completedCourseIds,
                                                 List<LineView> availableLines,
                                                 String nextCursor, boolean hasNext) {
        List<MyCourseCardResponse> cards = myCourses.stream()
                .map(myCourse -> new MyCourseCardResponse(
                        myCourse.getCourseId(),
                        myCourse.getName(),
                        myCourse.getStationId(),
                        myCourse.getStationName(),
                        toLine(myCourse.getLineId(), myCourse.getLineName(), myCourse.getLineCode()),
                        completedCourseIds.contains(myCourse.getCourseId())))
                .toList();
        List<LineSummaryResponse> lineFilters = availableLines.stream()
                .map(line -> new LineSummaryResponse(line.getLineId(), line.getLineName(), line.getLineCode()))
                .toList();
        return new MyCourseListResponse(lineFilters, cards, nextCursor, hasNext);
    }

    // 대표 호선(station.draw_line)은 뽑기 대상이 아닌 역에서 비어 있을 수 있어 LEFT JOIN으로 조회한다.
    // 그 경우 id/name/code가 모두 null이므로 노선 객체 자체를 null로 내린다.
    private LineSummaryResponse toLine(Long lineId, String lineName, LineCode lineCode) {
        if (lineId == null) {
            return null;
        }
        return new LineSummaryResponse(lineId, lineName, lineCode);
    }

    public PlaceCourseResponse toPlaceCourseResponse(PlaceCourseView course, int placeCount,
                                                     List<String> tags, String imageUrl) {
        return new PlaceCourseResponse(
                course.getCourseId(),
                course.getName(),
                course.getStationId(),
                course.getStationName(),
                toLine(course.getLineId(), course.getLineName(), course.getLineCode()),
                placeCount,
                estimateDuration(placeCount),
                tags,
                imageUrl
        );
    }

    /**
     * 장소 수로 소요시간 구간을 추정한다 (3~4곳 → 3~4시간, 5~7곳 → 반나절, 8곳 이상 → 하루종일).
     * <p>
     * 원래는 실제로 다녀온 사람이 여행기록에 남긴 "코스 시간"을 쓰는 게 맞다.
     * 이 목록은 공개된 여행일지가 있는 코스만 노출하므로 그 값이 항상 존재한다.
     * 다만 아직 여행일지에 소요시간 컬럼이 없어, 들어오기 전까지만 장소 수로 대신 추정한다.
     * 그때 값만 갈아끼우면 되도록 여행기록 선택지와 같은 구간을 쓴다.
     */
    private String estimateDuration(int placeCount) {
        if (placeCount >= FULL_DAY_MIN_PLACES) {
            return DURATION_FULL_DAY;
        }
        if (placeCount >= HALF_DAY_MIN_PLACES) {
            return DURATION_HALF_DAY;
        }
        return DURATION_SHORT;
    }

    public List<PopularCourseResponse> toPopularResponses(List<Course> courses, Set<Long> likedCourseIds) {
        return courses.stream()
                .map(course -> new PopularCourseResponse(
                        course.getId(),
                        course.getName(),
                        course.getViewCount(),
                        course.getLikeCount(),
                        likedCourseIds.contains(course.getId())
                ))
                .toList();
    }
}
