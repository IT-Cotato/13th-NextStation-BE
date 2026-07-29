package com.cotato.nextstation.domain.journal.service.query;

import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceInfoResponse;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.domain.journal.dto.response.JournalWriteInfoResponse;
import com.cotato.nextstation.domain.journal.dto.response.UncompletedJournalListResponse;
import com.cotato.nextstation.domain.journal.repository.JournalRepository;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.service.query.PlaceInfoQueryService;
import com.cotato.nextstation.domain.stamp.entity.MemberStamp;
import com.cotato.nextstation.domain.stamp.service.query.MemberStampQueryService;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import com.cotato.nextstation.domain.station.service.query.StationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalQueryService {

    private static final int TAGS_PER_CARD = 2;

    private final MemberStampQueryService memberStampQueryService;
    private final CourseQueryService courseQueryService;
    private final PlaceInfoQueryService placeInfoQueryService;
    private final StationQueryService stationQueryService;

    private final StationRepository stationRepository;
    private final JournalRepository journalRepository;

    public JournalWriteInfoResponse getWriteInfo(Long memberId, Long memberStampId) {
        // 1. memberStampId → courseId
        Long courseId = memberStampQueryService.getCourseId(memberId, memberStampId);

        // 2. courseId → 코스 정보 (courseName, stationId)
        CourseInfoResponse courseInfo = courseQueryService.getCourseInfo(courseId);

        // 3. stationId → stationName
        String stationName = stationRepository.findById(courseInfo.stationId())
                .map(station -> station.getStationName())
                .orElse(null);

        // 4. courseId → 장소 목록 (placeId + orderNum)
        List<CoursePlaceInfoResponse> coursePlaces = courseQueryService.getCoursePlaces(courseId);
        List<Long> placeIds = coursePlaces.stream()
                .map(CoursePlaceInfoResponse::placeId)
                .toList();

        // 5. placeIds → 장소 이름
        Map<Long, PlaceInfoResponse> placeInfoMap = placeInfoQueryService.getPlaceInfos(placeIds)
                .stream()
                .collect(Collectors.toMap(PlaceInfoResponse::placeId, Function.identity()));

        // 6. placeIds → 태그 상위 3개
        List<String> tags = placeInfoQueryService.getTopTagNames(placeIds);

        return JournalWriteInfoResponse.of(stationName, courseInfo.name(), tags, coursePlaces, placeInfoMap);
    }

    // 여행일지 미작성 코스 조회
    public UncompletedJournalListResponse getUncompletedJournals(Long memberId) {
        // 1. 이미 일지가 작성된 memberStampId 목록 (journal 도메인 내부에서 처리)
        Set<Long> completedStampIds = journalRepository.findCompletedMemberStampIdsByMemberId(memberId);

        // 2. 미작성 스탬프 목록 조회 (최신순)
        List<MemberStamp> uncompletedStamps =
                memberStampQueryService.getUncompletedStamps(memberId, completedStampIds);

        if (uncompletedStamps.isEmpty()) {
            return new UncompletedJournalListResponse(0, List.of());
        }

        // 3. courseId 목록 추출
        List<Long> courseIds = uncompletedStamps.stream()
                .map(MemberStamp::getCourseId)
                .toList();

        // 4. courseId → 코스 정보 (courseName, stationId)
        // TODO: CourseQueryService에 배치 조회 메서드 생기면 N+1 개선 가능
        Map<Long, CourseInfoResponse> courseInfoMap = courseIds.stream()
                .map(courseQueryService::getCourseInfo)
                .collect(Collectors.toMap(CourseInfoResponse::courseId, Function.identity()));

        // 5. stationId → stationName
        Set<Long> stationIds = courseInfoMap.values().stream()
                .map(CourseInfoResponse::stationId)
                .collect(Collectors.toSet());
        Map<Long, String> stationNameMap = stationQueryService.getStationNames(stationIds);

        // 6. courseId → placeIds → 태그 2개
        // TODO: CourseQueryService에 배치 조회 메서드 생기면 N+1 개선 가능
        Map<Long, List<String>> tagsByCourse = new HashMap<>();
        for (Long courseId : courseIds) {
            List<Long> placeIds = courseQueryService.getCoursePlaces(courseId).stream()
                    .map(CoursePlaceInfoResponse::placeId)
                    .toList();
            List<String> tags = placeInfoQueryService.getTopTagNames(placeIds).stream()
                    .limit(TAGS_PER_CARD)
                    .toList();
            tagsByCourse.put(courseId, tags);
        }

        // 7. 응답 조합
        List<UncompletedJournalListResponse.UncompletedCourseResponse> courses =
                uncompletedStamps.stream()
                        .map(stamp -> {
                            CourseInfoResponse courseInfo = courseInfoMap.get(stamp.getCourseId());
                            String stationName = stationNameMap.get(courseInfo.stationId());
                            List<String> tags = tagsByCourse.get(stamp.getCourseId());

                            return new UncompletedJournalListResponse.UncompletedCourseResponse(
                                    stamp.getId(),
                                    stationName,
                                    courseInfo.name(),
                                    tags,
                                    stamp.getCreatedAt()
                            );
                        })
                        .toList();

        return new UncompletedJournalListResponse(courses.size(), courses);
    }

}