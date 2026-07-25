package com.cotato.nextstation.domain.course.service.query;

import com.cotato.nextstation.domain.course.converter.CourseConverter;
import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.MyCourseListResponse;
import com.cotato.nextstation.domain.course.dto.response.PlaceCourseResponse;
import com.cotato.nextstation.domain.course.dto.response.PopularCourseResponse;
import com.cotato.nextstation.domain.course.dto.response.SavedCourseListResponse;
import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.course.entity.CoursePlace;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.repository.CoursePlaceRepository;
import com.cotato.nextstation.domain.course.repository.CourseRepository;
import com.cotato.nextstation.domain.course.repository.CourseRepository.LineView;
import com.cotato.nextstation.domain.course.repository.CourseRepository.MyCourseView;
import com.cotato.nextstation.domain.course.repository.CourseRepository.PlaceCourseView;
import com.cotato.nextstation.domain.course.repository.CourseSaveRepository;
import com.cotato.nextstation.domain.course.repository.CourseSaveRepository.SavedCourseView;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.service.query.PlaceInfoQueryService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import com.cotato.nextstation.global.util.CursorData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 코스 조회 전용 서비스.
// 저장 탭 목록 같은 화면 조회와, 다른 도메인이 코스를 참조할 때 쓰는 포트를 함께 제공한다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryService {

    private static final int DEFAULT_SIZE = 10;

    // TODO: 방어적으로 잡은 값. 추후 프론트 실제 요청 사이즈 확인 후 조정 필요.
    private static final int MAX_SIZE = 50;

    // 장소 상세 화면의 "이 장소를 포함한 코스"는 가로 스크롤 6개 고정이라 더보기가 없다.
    private static final int PLACE_COURSE_LIMIT = 6;

    // 카드에 노출할 태그 수 (디자인상 2개)
    private static final int TAGS_PER_CARD = 2;

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final CourseSaveRepository courseSaveRepository;
    private final PlaceInfoQueryService placeInfoQueryService;
    private final CourseConverter courseConverter;

    /**
     * 저장 탭 - 스크랩한 코스 목록 (최근 스크랩순).
     * 원본이 삭제되거나 비공개로 바뀐 코스는 조회 단계에서 빠진다.
     * 화면에 필터 칩이 없어 필터 파라미터를 받지 않는다.
     */
    public SavedCourseListResponse getSavedCourses(Long memberId, String cursor, Integer size) {
        int pageSize = resolvePageSize(size);
        Pageable pageable = PageRequest.of(0, pageSize + 1); // hasNext 판단용 1개 더 조회

        // 커서 해석은 한 번만 한다. 빈 문자열도 "커서 없음"으로 취급된다.
        CursorData cursorData = CursorData.decode(cursor);
        List<SavedCourseView> savedCourses = fetchSavedCourses(memberId, cursorData, pageable);

        boolean hasNext = savedCourses.size() > pageSize;
        List<SavedCourseView> pageContent = hasNext ? savedCourses.subList(0, pageSize) : savedCourses;

        String nextCursor = null;
        if (hasNext) {
            SavedCourseView last = pageContent.get(pageContent.size() - 1);
            nextCursor = new CursorData(last.getSaveId(), null, last.getSavedAt()).encode();
        }
        return courseConverter.toSavedListResponse(pageContent, nextCursor, hasNext);
    }

    private List<SavedCourseView> fetchSavedCourses(Long memberId, CursorData cursorData, Pageable pageable) {
        if (cursorData == null) {
            return courseSaveRepository.findSavedCourses(memberId, pageable);
        }
        validateTimeCursor(cursorData);
        return courseSaveRepository.findSavedCoursesAfterCursor(
                memberId, cursorData.dateTimeValue(), cursorData.id(), pageable);
    }

    /**
     * 저장 탭 - 내가 만든 코스 목록 (최신순).
     * 본인 코스이므로 공개 여부와 무관하게 전부 보여준다.
     * 호선/역 필터는 선택 사항이고, 필터 칩 활성화에 쓸 availableLines는 최초 조회에서만 채운다.
     */
    public MyCourseListResponse getMyCourses(Long memberId, Long lineId, Long stationId,
                                             String cursor, Integer size) {
        int pageSize = resolvePageSize(size);
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        // 커서 해석은 한 번만 한다. 빈 문자열도 "커서 없음"으로 취급된다.
        CursorData cursorData = CursorData.decode(cursor);
        List<MyCourseView> myCourses = fetchMyCourses(memberId, lineId, stationId, cursorData, pageable);

        boolean hasNext = myCourses.size() > pageSize;
        List<MyCourseView> pageContent = hasNext ? myCourses.subList(0, pageSize) : myCourses;

        String nextCursor = null;
        if (hasNext) {
            MyCourseView last = pageContent.get(pageContent.size() - 1);
            nextCursor = new CursorData(last.getCourseId(), null, last.getCreatedAt()).encode();
        }

        // 필터 칩은 화면에 한 번만 그리므로 최초 조회에서만 계산한다 (totalCount와 같은 방식)
        List<LineView> availableLines = (cursorData == null)
                ? courseRepository.findAvailableLines(memberId)
                : List.of();

        return courseConverter.toMyListResponse(pageContent, availableLines, nextCursor, hasNext);
    }

    private List<MyCourseView> fetchMyCourses(Long memberId, Long lineId, Long stationId,
                                              CursorData cursorData, Pageable pageable) {
        if (cursorData == null) {
            return courseRepository.findMyCourses(memberId, lineId, stationId, pageable);
        }
        validateTimeCursor(cursorData);
        return courseRepository.findMyCoursesAfterCursor(
                memberId, lineId, stationId, cursorData.dateTimeValue(), cursorData.id(), pageable);
    }

    // 두 목록 모두 시간순 정렬이라 커서에는 시각과 id만 들어 있어야 한다.
    private void validateTimeCursor(CursorData cursorData) {
        if (cursorData.id() == null || cursorData.dateTimeValue() == null || cursorData.longValue() != null) {
            throw new CustomException(GlobalErrorCode.INVALID_CURSOR);
        }
    }

    private int resolvePageSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new CustomException(GlobalErrorCode.INVALID_PAGE_SIZE);
        }
        return size;
    }

    /**
     * 장소 상세 화면 하단의 "이 장소를 포함한 코스".
     * 인기순 상위 6개를 뽑되 노출 순서는 매번 섞는다. 같은 장소를 다시 열었을 때
     * 늘 같은 순서면 아래쪽 코스가 계속 묻히기 때문이다.
     */
    public List<PlaceCourseResponse> getCoursesByPlace(Long placeId) {
        List<PlaceCourseView> courses = new ArrayList<>(courseRepository.findPopularPublicCoursesByPlaceId(
                placeId, PageRequest.of(0, PLACE_COURSE_LIMIT)));
        if (courses.isEmpty()) {
            return List.of();
        }
        Collections.shuffle(courses);

        List<Long> courseIds = courses.stream().map(PlaceCourseView::getCourseId).toList();
        Map<Long, List<Long>> placeIdsByCourse = groupPlaceIdsByCourse(courseIds);
        Map<Long, String> imageUrlByCourse = resolveCoverImages(placeIdsByCourse);

        return courses.stream()
                .map(course -> {
                    List<Long> placeIds = placeIdsByCourse.getOrDefault(course.getCourseId(), List.of());
                    return courseConverter.toPlaceCourseResponse(
                            course, placeIds.size(), resolveCourseTags(placeIds), imageUrlByCourse.get(course.getCourseId()));
                })
                .toList();
    }

    // 코스별 장소 id를 순서대로 묶는다. 장소 수·대표 이미지·태그가 모두 여기서 나온다.
    private Map<Long, List<Long>> groupPlaceIdsByCourse(List<Long> courseIds) {
        return coursePlaceRepository.findByCourseIdInOrderByCourseIdAscOrderNumAsc(courseIds).stream()
                .collect(Collectors.groupingBy(
                        CoursePlace::getCourseId,
                        LinkedHashMap::new,
                        Collectors.mapping(CoursePlace::getPlaceId, Collectors.toList())
                ));
    }

    // 카드 배경은 코스의 첫 장소 이미지를 쓴다. 장소 이미지가 없을 때의 폴백은 장소 조회 쪽에서 처리된다.
    private Map<Long, String> resolveCoverImages(Map<Long, List<Long>> placeIdsByCourse) {
        Map<Long, Long> firstPlaceByCourse = new LinkedHashMap<>();
        placeIdsByCourse.forEach((courseId, placeIds) -> {
            if (!placeIds.isEmpty()) {
                firstPlaceByCourse.put(courseId, placeIds.get(0));
            }
        });
        if (firstPlaceByCourse.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> imageUrlByPlace = placeInfoQueryService.getPlaceInfos(List.copyOf(firstPlaceByCourse.values()))
                .stream()
                .filter(place -> place.imageUrl() != null)
                .collect(Collectors.toMap(PlaceInfoResponse::placeId, PlaceInfoResponse::imageUrl));

        Map<Long, String> result = new LinkedHashMap<>();
        firstPlaceByCourse.forEach((courseId, placeId) -> result.put(courseId, imageUrlByPlace.get(placeId)));
        return result;
    }

    // 코스 대표 태그 = 그 코스 장소들의 태그를 집계한 상위 태그.
    // 집계는 장소 도메인이 맡고 있어 코스마다 한 번씩 호출한다(코스가 6개로 고정이라 호출 수도 고정).
    private List<String> resolveCourseTags(List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return List.of();
        }
        return placeInfoQueryService.getTopTagNames(placeIds).stream()
                .limit(TAGS_PER_CARD)
                .toList();
    }

    public CourseInfoResponse getCourseInfo(Long courseId) {
        return courseConverter.toInfoResponse(findCourse(courseId));
    }

    public List<CoursePlaceInfoResponse> getCoursePlaces(Long courseId) {
        findCourse(courseId);
        return courseConverter.toPlaceInfoResponses(coursePlaceRepository.findByCourseIdOrderByOrderNumAsc(courseId));
    }

    // 역별 인기 코스 상위 limit개
    // 공개된 여행일지가 있는 코스만 노출한다
    // 스탬프 페이지·둘러보기 등 다른 도메인이 Course에 직접 의존하지 않고 이 메서드를 호출한다
    public List<PopularCourseResponse> getPopularCoursesByStation(Long stationId, int limit) {
        return getPopularCoursesByStation(stationId, limit, null);
    }

    // memberId를 넘기면 응답의 isSaved가 채워진다. null이면 전부 false.
    public List<PopularCourseResponse> getPopularCoursesByStation(Long stationId, int limit, Long memberId) {
        List<Course> courses = courseRepository.findPopularPublicCoursesByStationId(stationId, PageRequest.of(0, limit));
        return courseConverter.toPopularResponses(courses, resolveSavedCourseIds(memberId, courses));
    }

    // 조회한 코스들의 스크랩 여부를 한 번에 조회한다 (코스마다 조회하면 N+1)
    private Set<Long> resolveSavedCourseIds(Long memberId, List<Course> courses) {
        if (memberId == null || courses.isEmpty()) {
            return Set.of();
        }
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        return Set.copyOf(courseSaveRepository.findSavedCourseIds(memberId, courseIds));
    }

    private Course findCourse(Long courseId) {
        // 삭제된 코스는 Course의 @SQLRestriction 으로 조회에서 자동 제외된다
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
    }
}
