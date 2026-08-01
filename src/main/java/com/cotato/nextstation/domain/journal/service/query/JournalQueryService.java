package com.cotato.nextstation.domain.journal.service.query;

import com.cotato.nextstation.domain.course.dto.response.CourseInfoResponse;
import com.cotato.nextstation.domain.course.dto.response.CoursePlaceInfoResponse;
import com.cotato.nextstation.domain.course.service.command.CourseCommandService;
import com.cotato.nextstation.domain.course.service.query.CourseQueryService;
import com.cotato.nextstation.domain.journal.converter.JournalConverter;
import com.cotato.nextstation.domain.journal.dto.response.JournalDetailResponse;
import com.cotato.nextstation.domain.journal.dto.response.JournalWriteInfoResponse;
import com.cotato.nextstation.domain.journal.dto.response.UncompletedJournalListResponse;
import com.cotato.nextstation.domain.journal.entity.Journal;
import com.cotato.nextstation.domain.journal.entity.JournalImage;
import com.cotato.nextstation.domain.journal.enums.TravelDuration;
import com.cotato.nextstation.domain.journal.exception.JournalErrorCode;
import com.cotato.nextstation.domain.journal.repository.JournalImageRepository;
import com.cotato.nextstation.domain.journal.repository.JournalRepository;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.place.entity.PlaceReviewImage;
import com.cotato.nextstation.domain.place.repository.PlaceReviewImageRepository;
import com.cotato.nextstation.domain.place.repository.PlaceReviewRepository;
import com.cotato.nextstation.domain.place.service.query.PlaceInfoQueryService;
import com.cotato.nextstation.domain.stamp.entity.MemberStamp;
import com.cotato.nextstation.domain.stamp.service.query.MemberStampQueryService;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.service.query.StationQueryService;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalQueryService {

    private static final int TAGS_PER_CARD = 2;

    private final MemberStampQueryService memberStampQueryService;
    private final CourseQueryService courseQueryService;
    private final CourseCommandService courseCommandService;
    private final PlaceInfoQueryService placeInfoQueryService;
    private final StationQueryService stationQueryService;

    private final JournalRepository journalRepository;
    private final JournalImageRepository journalImageRepository;
    private final PlaceReviewRepository placeReviewRepository;
    private final PlaceReviewImageRepository placeReviewImageRepository;

    private final JournalConverter journalConverter;


    // 여행일지 작성 초기 정보 조회
    public JournalWriteInfoResponse getWriteInfo(Long memberId, Long memberStampId) {
        // 1. memberStampId → courseId
        Long courseId = memberStampQueryService.getCourseId(memberId, memberStampId);

        // 2. courseId → 코스 정보 (courseName, stationId)
        CourseInfoResponse courseInfo = courseQueryService.getCourseInfo(courseId);

        // 3. stationId → stationName
        String stationName = stationQueryService.getStationName(courseInfo.stationId());

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

        return journalConverter.toWriteInfoResponse(stationName, courseInfo.name(), tags, coursePlaces, placeInfoMap);
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


        return journalConverter.toUncompletedJournalListResponse(
                uncompletedStamps, courseInfoMap, stationNameMap, tagsByCourse);
    }


    // 여행일지 상세 조회
    public JournalDetailResponse getJournalDetail(Long memberId, Long journalId) {
        // 1. 여행일지 조회
        Journal journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new CustomException(JournalErrorCode.JOURNAL_NOT_FOUND));

        // 2. 본인 여부 확인 → 타인이면 공개 일지만 조회 가능
        boolean isOwner = journal.getMember().getId().equals(memberId);
        if (!isOwner && !journal.isPublic()) {
            throw new CustomException(JournalErrorCode.JOURNAL_FORBIDDEN);
        }

        // 3. memberStampId → courseId
        Long courseId = memberStampQueryService.getCourseId(
                journal.getMember().getId(), journal.getMemberStampId());

        // 4. courseId → 코스 정보 (courseName, stationId, viewCount, saveCount)
        CourseInfoResponse courseInfo = courseQueryService.getCourseInfo(courseId);

        // 5. stationId → stationName, line
       String stationName = stationQueryService
                .getStationName(courseInfo.stationId());
        LineSummaryResponse line = stationQueryService.getLine(courseInfo.stationId());

        // 6. courseId → 장소 목록 (placeId + orderNum)
        List<CoursePlaceInfoResponse> coursePlaces = courseQueryService.getCoursePlaces(courseId);
        List<Long> placeIds = coursePlaces.stream()
                .map(CoursePlaceInfoResponse::placeId)
                .toList();

        // 7. placeIds → 장소 이름
        Map<Long, PlaceInfoResponse> placeInfoMap = placeInfoQueryService.getPlaceInfos(placeIds)
                .stream()
                .collect(Collectors.toMap(PlaceInfoResponse::placeId, Function.identity()));

        // 8. placeIds → 태그 상위 3개
        List<String> tags = placeInfoQueryService.getTopTagNames(placeIds);

        // 9. journalId → 대표 사진 + 서브 사진
        List<String> imageUrls = journalImageRepository.findByJournalIdOrderByIdAsc(journalId)
                .stream()
                .map(JournalImage::getImageUrl)
                .toList();

        // 10. journalId → 장소 리뷰 + 리뷰 이미지
        List<PlaceReview> placeReviews = placeReviewRepository.findByJournalId(journalId);
        Map<Long, PlaceReview> reviewByPlaceId = placeReviews.stream()
                .collect(Collectors.toMap(pr -> pr.getPlace().getId(), Function.identity()));

        List<Long> reviewIds = placeReviews.stream().map(PlaceReview::getId).toList();
        Map<Long, String> imageUrlByReviewId = placeReviewImageRepository
                .findByPlaceReviewIdIn(reviewIds).stream()
                .collect(Collectors.toMap(
                        pri -> pri.getPlaceReview().getId(),
                        PlaceReviewImage::getImageUrl,
                        (a, b) -> a  // 기획상 1개만이라 중복 시 첫 번째 사용
                ));

        // 11. 조회수 반영 (본인 조회는 CourseCommandService 내부에서 제외) + 좋아요 여부
        //
        // increaseViewCount가 반환하는 값을 그대로 써야 한다. 이 트랜잭션(readOnly)에서 courseInfo를
        // 다시 조회해도 REPEATABLE READ 스냅샷 때문에 증가 전 값이 보인다 — REQUIRES_NEW로 증가를 수행한
        // 그 트랜잭션 안에서 읽은 값만 증가분을 반영하고 있다. 실패(또는 본인 조회로 no-op)했으면
        // null이 오므로 4번에서 조회해 둔 courseInfo의 값으로 대체한다.
        Integer updatedViewCount = courseCommandService.increaseViewCount(courseId, memberId);
        boolean isLiked = courseQueryService.isLikedByMember(courseId, memberId);
        int viewCount = updatedViewCount != null ? updatedViewCount : courseInfo.viewCount();

        return journalConverter.toJournalDetailResponse(
                journal, line, stationName, courseInfo, viewCount, isOwner, isLiked, tags, imageUrls,
                coursePlaces, placeInfoMap, reviewByPlaceId, imageUrlByReviewId);
    }

    // Course 도메인이 코스 카드의 소요시간(travel_duration) 표시를 위함
    public TravelDuration getTravelDuration(Long journalId) {
        // course.journal_id가 nullable이라 journalId가 null로 들어올 수 있다. findById(null)은 예외라 먼저 막는다.
        if (journalId == null) {
            return null;
        }
        return journalRepository.findById(journalId)
                .map(Journal::getTravelDuration)
                .orElse(null);  // 일지 없거나 미작성이면 null (장소 수로 추정)
    }






}