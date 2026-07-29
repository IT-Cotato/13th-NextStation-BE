package com.cotato.nextstation.domain.course.service.query;

import com.cotato.nextstation.domain.course.converter.CourseConverter;
import com.cotato.nextstation.domain.course.dto.request.ExploreCourseCondition;
import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.ExploreCourseListResponse;
import com.cotato.nextstation.domain.course.dto.response.ExploreCourseResponse;
import com.cotato.nextstation.domain.course.dto.response.MyCourseListResponse;
import com.cotato.nextstation.domain.course.dto.response.PlaceCourseResponse;
import com.cotato.nextstation.domain.course.dto.response.PopularCourseResponse;
import com.cotato.nextstation.domain.course.dto.response.LikedCourseListResponse;
import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.course.entity.CoursePlace;
import com.cotato.nextstation.domain.course.entity.CourseSort;
import com.cotato.nextstation.domain.course.exception.CourseErrorCode;
import com.cotato.nextstation.domain.course.repository.CoursePlaceRepository;
import com.cotato.nextstation.domain.course.repository.CourseRepository;
import com.cotato.nextstation.domain.course.repository.CourseRepository.LineView;
import com.cotato.nextstation.domain.course.repository.CourseRepository.ExploreCourseView;
import com.cotato.nextstation.domain.course.repository.CourseRepository.MyCourseView;
import com.cotato.nextstation.domain.course.repository.CourseRepository.PlaceCourseView;
import com.cotato.nextstation.domain.course.repository.CourseLikeRepository;
import com.cotato.nextstation.domain.course.repository.CourseLikeRepository.LikedCourseView;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.service.query.PlaceInfoQueryService;
import com.cotato.nextstation.domain.stamp.service.query.MemberStampQueryService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import com.cotato.nextstation.global.util.CursorData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.LocalDateTime;
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

    // 모든 역명이 이 접미사로 끝나 검색어로서 변별력이 없다. 역 검색과 같은 규칙으로 뗀다.
    private static final String STATION_NAME_SUFFIX = "역";

    // "사람들이 많이 찾는 코스"는 상위 30개까지만 보여준다(무한스크롤도 여기서 끝난다).
    private static final int MOST_LIKED_LIMIT = 30;

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final CourseLikeRepository courseLikeRepository;
    private final PlaceInfoQueryService placeInfoQueryService;
    private final MemberStampQueryService memberStampQueryService;
    private final CourseConverter courseConverter;

    /**
     * 좋아요(하트)한 코스 목록 (최근 좋아요순).
     * 원본이 삭제되거나 비공개로 바뀐 코스는 조회 단계에서 빠진다.
     * 화면에 필터 칩이 없어 필터 파라미터를 받지 않는다.
     */
    public LikedCourseListResponse getLikedCourses(Long memberId, String cursor, Integer size) {
        int pageSize = resolvePageSize(size);
        Pageable pageable = PageRequest.of(0, pageSize + 1); // hasNext 판단용 1개 더 조회

        // 커서 해석은 한 번만 한다. 빈 문자열도 "커서 없음"으로 취급된다.
        CursorData cursorData = CursorData.decode(cursor);
        List<LikedCourseView> likedCourses = fetchLikedCourses(memberId, cursorData, pageable);

        boolean hasNext = likedCourses.size() > pageSize;
        List<LikedCourseView> pageContent = hasNext ? likedCourses.subList(0, pageSize) : likedCourses;

        String nextCursor = null;
        if (hasNext) {
            LikedCourseView last = pageContent.get(pageContent.size() - 1);
            nextCursor = new CursorData(last.getLikeId(), null, last.getLikedAt()).encode();
        }
        return courseConverter.toLikedListResponse(pageContent, nextCursor, hasNext);
    }

    private List<LikedCourseView> fetchLikedCourses(Long memberId, CursorData cursorData, Pageable pageable) {
        if (cursorData == null) {
            return courseLikeRepository.findLikedCourses(memberId, pageable);
        }
        validateTimeCursor(cursorData);
        return courseLikeRepository.findLikedCoursesAfterCursor(
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

        // 카드 스탬프 모양이 여행 완료 여부에 따라 달라진다. 페이지에 실린 코스만 한 번에 확인한다.
        List<Long> courseIds = pageContent.stream().map(MyCourseView::getCourseId).toList();
        Set<Long> completedCourseIds = memberStampQueryService.getCompletedCourseIds(memberId, courseIds);

        return courseConverter.toMyListResponse(pageContent, completedCourseIds, availableLines, nextCursor, hasNext);
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
     * 둘러보기 코스 목록. 노선따라 둘러보기와 코스 검색이 같은 조회를 쓴다.
     * <p>
     * 공개된 여행일지가 있는 코스만 나온다. 카드를 누르면 여행일지 상세로 가므로
     * 응답에 journalId를 함께 내린다.
     * <p>
     * 필터·검색어는 모두 선택 사항이고, 정렬은 최신순이 기본이다.
     * {@code memberId}는 하트를 채울지 판단하는 데만 쓰며 비로그인이면 null이다.
     */
    public ExploreCourseListResponse getExploreCourses(Long memberId, ExploreCourseCondition condition,
                                                       CourseSort sort, String cursor, Integer size) {
        int pageSize = resolvePageSize(size);
        Pageable pageable = PageRequest.of(0, pageSize + 1); // hasNext 판단용 1개 더 조회
        CourseSort resolvedSort = (sort == null) ? CourseSort.LATEST : sort;

        CursorData cursorData = CursorData.decode(cursor);
        List<ExploreCourseView> courses = fetchExploreCourses(condition, resolvedSort, cursorData, pageable);

        boolean hasNext = courses.size() > pageSize;
        List<ExploreCourseView> pageContent = hasNext ? courses.subList(0, pageSize) : courses;

        String nextCursor = null;
        if (hasNext) {
            nextCursor = encodeExploreCursor(pageContent.get(pageContent.size() - 1), resolvedSort);
        }
        return courseConverter.toExploreListResponse(toExploreCards(memberId, pageContent), nextCursor, hasNext);
    }

    /**
     * 사람들이 많이 찾는 코스. 좋아요 수 내림차순으로 상위 {@value #MOST_LIKED_LIMIT}개까지만 보여준다.
     * <p>
     * 둘러보기 목록의 "인기순"(조회수 + 좋아요 × 2)과는 다른 기준이다. 화면 부제가
     * "가장 많이 담아둔 코스"라 담은 횟수만 본다.
     * <p>
     * 상한이 고정이라 커서에 정렬값 대신 다음 시작 위치를 담는다. 좋아요 수는 수시로 바뀌어서
     * 값 기준 커서를 쓰면 순위가 흔들릴 때 같은 코스가 두 번 나오거나 빠진다.
     * 30개짜리 고정 차트라 위치로 끊는 편이 단순하고 결과도 예측 가능하다.
     */
    public ExploreCourseListResponse getMostLikedCourses(Long memberId, String cursor, Integer size) {
        int pageSize = resolvePageSize(size);
        int offset = resolveOffsetCursor(cursor);

        // 상한이 30개라 전부 가져와 잘라 쓴다. 페이지마다 다시 읽어도 30행이라 부담이 없다.
        List<ExploreCourseView> topCourses =
                courseRepository.findMostLikedCourses(PageRequest.of(0, MOST_LIKED_LIMIT));

        if (offset >= topCourses.size()) {
            return courseConverter.toExploreListResponse(List.of(), null, false);
        }

        int end = Math.min(offset + pageSize, topCourses.size());
        List<ExploreCourseView> pageContent = topCourses.subList(offset, end);
        boolean hasNext = end < topCourses.size();
        String nextCursor = hasNext ? new CursorData(null, (long) end, null).encode() : null;

        return courseConverter.toExploreListResponse(toExploreCards(memberId, pageContent), nextCursor, hasNext);
    }

    // 이 목록의 커서는 정렬값이 아니라 다음 시작 위치다.
    private int resolveOffsetCursor(String cursor) {
        CursorData cursorData = CursorData.decode(cursor);
        if (cursorData == null) {
            return 0;
        }
        if (cursorData.longValue() == null || cursorData.longValue() < 0) {
            throw new CustomException(GlobalErrorCode.INVALID_CURSOR);
        }
        return cursorData.longValue().intValue();
    }

    private List<ExploreCourseView> fetchExploreCourses(ExploreCourseCondition condition, CourseSort sort,
                                                        CursorData cursorData, Pageable pageable) {
        if (cursorData != null) {
            validateExploreCursor(cursorData, sort);
        }
        Long courseId = (cursorData == null) ? null : cursorData.id();
        LocalDateTime createdAt = (cursorData == null) ? null : cursorData.dateTimeValue();

        String keyword = normalizeKeyword(condition.keyword());
        if (sort == CourseSort.POPULAR) {
            Long score = (cursorData == null) ? null : cursorData.longValue();
            return courseRepository.findExploreCoursesByPopular(condition.lineId(), condition.stationId(),
                    keyword, condition.conceptTourId(), score, createdAt, courseId, pageable);
        }
        return courseRepository.findExploreCoursesByLatest(condition.lineId(), condition.stationId(),
                keyword, condition.conceptTourId(), createdAt, courseId, pageable);
    }

    // 커서가 정렬과 맞는지 확인한다. 인기순 커서에는 점수가 들어 있고 최신순에는 없다.
    // 정렬을 바꾸면서 이전 커서를 그대로 보내면 순서가 뒤엉키므로 아예 막는다.
    private void validateExploreCursor(CursorData cursorData, CourseSort sort) {
        boolean scoreRequired = (sort == CourseSort.POPULAR);
        boolean malformed = cursorData.id() == null
                || cursorData.dateTimeValue() == null
                || scoreRequired != (cursorData.longValue() != null);
        if (malformed) {
            throw new CustomException(GlobalErrorCode.INVALID_CURSOR);
        }
    }

    private String encodeExploreCursor(ExploreCourseView last, CourseSort sort) {
        Long score = (sort == CourseSort.POPULAR)
                ? (long) (last.getViewCount() + last.getLikeCount() * 2)
                : null;
        return new CursorData(last.getCourseId(), score, last.getCreatedAt()).encode();
    }

    // 역 검색과 같은 규칙으로 다듬는다. 모든 역명이 "역"으로 끝나 꼬리의 "역"은 역을 구분하지 못한다.
    // "역" 한 글자는 떼지 않는다. 역삼역처럼 이름에 "역"이 든 역을 찾으려는 입력이다.
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.length() > STATION_NAME_SUFFIX.length() && trimmed.endsWith(STATION_NAME_SUFFIX)) {
            return trimmed.substring(0, trimmed.length() - STATION_NAME_SUFFIX.length());
        }
        return trimmed;
    }

    // 카드에 필요한 태그와 좋아요 여부를 페이지 단위로 한 번에 채운다 (코스마다 조회하면 N+1).
    private List<ExploreCourseResponse> toExploreCards(Long memberId, List<ExploreCourseView> courses) {
        if (courses.isEmpty()) {
            return List.of();
        }

        List<Long> courseIds = courses.stream().map(ExploreCourseView::getCourseId).toList();
        Map<Long, List<Long>> placeIdsByCourse = groupPlaceIdsByCourse(courseIds);
        Map<Long, List<String>> tagsByCourse = resolveTagsByCourse(placeIdsByCourse);
        Set<Long> likedCourseIds = resolveLikedCourseIds(memberId, courseIds);

        return courses.stream()
                .map(course -> courseConverter.toExploreCourseResponse(
                        course,
                        tagsByCourse.getOrDefault(course.getCourseId(), List.of()),
                        likedCourseIds.contains(course.getCourseId()),
                        null)) // TODO: 여행일지 대표 사진. JournalImage 머지 후 연결한다
                .toList();
    }

    /**
     * 코스별 대표 태그를 한 번에 집계한다.
     * <p>
     * 장소 태그를 페이지 전체에 대해 한 번만 조회하고, 코스마다 담긴 장소들의 태그를 세서
     * 많이 나온 순으로 자른다. 코스별로 태그 조회를 부르면 페이지 크기만큼 쿼리가 나간다.
     */
    private Map<Long, List<String>> resolveTagsByCourse(Map<Long, List<Long>> placeIdsByCourse) {
        List<Long> allPlaceIds = placeIdsByCourse.values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();
        Map<Long, List<String>> tagsByPlace = placeInfoQueryService.getTagNamesByPlace(allPlaceIds);

        Map<Long, List<String>> result = new LinkedHashMap<>();
        placeIdsByCourse.forEach((courseId, placeIds) -> {
            Map<String, Long> tagCounts = placeIds.stream()
                    .flatMap(placeId -> tagsByPlace.getOrDefault(placeId, List.of()).stream())
                    .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
            result.put(courseId, tagCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(TAGS_PER_CARD)
                    .map(Map.Entry::getKey)
                    .toList());
        });
        return result;
    }

    private Set<Long> resolveLikedCourseIds(Long memberId, List<Long> courseIds) {
        if (memberId == null || courseIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(courseLikeRepository.findLikedCourseIds(memberId, courseIds));
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

    // memberId를 넘기면 응답의 isLiked가 채워진다. null이면 전부 false.
    public List<PopularCourseResponse> getPopularCoursesByStation(Long stationId, int limit, Long memberId) {
        List<Course> courses = courseRepository.findPopularPublicCoursesByStationId(stationId, PageRequest.of(0, limit));
        return courseConverter.toPopularResponses(courses, resolveLikedCourses(memberId, courses));
    }

    // 조회한 코스들의 좋아요 여부를 한 번에 조회한다 (코스마다 조회하면 N+1)
    private Set<Long> resolveLikedCourses(Long memberId, List<Course> courses) {
        return resolveLikedCourseIds(memberId, courses.stream().map(Course::getId).toList());
    }

    private Course findCourse(Long courseId) {
        // 삭제된 코스는 Course의 @SQLRestriction 으로 조회에서 자동 제외된다
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(CourseErrorCode.COURSE_NOT_FOUND));
    }
}
