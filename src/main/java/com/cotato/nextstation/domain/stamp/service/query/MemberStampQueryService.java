package com.cotato.nextstation.domain.stamp.service.query;

import com.cotato.nextstation.domain.journal.repository.JournalRepository;
import com.cotato.nextstation.domain.member.exception.MemberErrorCode;
import com.cotato.nextstation.domain.member.service.query.MemberExistenceQueryService;
import com.cotato.nextstation.domain.stamp.dto.response.MemberStampListResponse;
import com.cotato.nextstation.domain.stamp.dto.response.MemberStampResponse;
import com.cotato.nextstation.domain.stamp.entity.MemberStamp;
import com.cotato.nextstation.domain.stamp.exception.StampErrorCode;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository.MemberStampView;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 스탬프 조회 전용 서비스. 다른 도메인이 여행 완료 여부를 물어볼 때 이 서비스를 호출한다.
 * StampCourseQueryService가 CourseQueryService를 주입받고 있어, 코스 목록이 그 서비스를
 * 다시 호출하면 순환 참조가 된다. 그래서 완료 여부 조회만 담는 서비스를 따로 둔다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberStampQueryService {

    private final MemberStampRepository memberStampRepository;
    private final JournalRepository journalRepository;
    private final MemberExistenceQueryService memberExistenceQueryService;

    // 넘긴 코스들 중 회원이 완료한 코스 id 집합. 목록에서 카드별 완료 여부를 판단하는 데 쓴다.
    public Set<Long> getCompletedCourseIds(Long memberId, List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(memberStampRepository.findCompletedCourseIds(memberId, courseIds));
    }

    // memberStampId → courseId (소유권 검증 포함)
    public Long getCourseId(Long memberId, Long memberStampId) {
        MemberStamp memberStamp = memberStampRepository.findById(memberStampId)
                .orElseThrow(() -> new CustomException(StampErrorCode.MEMBER_STAMP_NOT_FOUND));

        if (!memberStamp.getMemberId().equals(memberId)) {
            // 존재 여부 노출 방지: 남의 스탬프도 NOT_FOUND로 응답
            throw new CustomException(StampErrorCode.MEMBER_STAMP_NOT_FOUND);
        }

        return memberStamp.getCourseId();
    }

    // 본인 스탬프인지 소유권 검증 (JournalCommandService에서 사용)
    public boolean existsByMemberIdAndId(Long memberId, Long memberStampId) {
        return memberStampRepository.existsByMemberIdAndId(memberId, memberStampId);
    }

    // 여행일지 미작성 스탬프. completedStampIds를 파라미터로 받아서 처리
    public List<MemberStamp> getUncompletedStamps(Long memberId, Set<Long> completedStampIds) {
        if (completedStampIds.isEmpty()) {
            return memberStampRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        }

        return memberStampRepository.findByMemberIdAndIdNotInOrderByCreatedAtDesc(
                memberId, completedStampIds);
    }

    // 다른 회원 프로필의 스탬프 개수(방문한 서로 다른 역의 개수). 호출부(회원 조회)에서 이미 존재 검증을 마쳤다고 가정한다.
    public long getStampCount(Long memberId) {
        return memberStampRepository.countVisitedStations(memberId);
    }

    /**
     * 다른 회원의 스탬프 탭. 방문한 역을 역 하나당 스탬프 1개로 묶어 조회한다.
     * <p>
     * 내 스탬프 목록(GET /stamps)과 같은 정렬 기준을 쓴다: 1호선 → 9호선 순, 대표 호선이
     * 없는 역은 맨 뒤. 동일 호선 내에서는 역명 가나다순으로 2차 정렬한다.
     * <p>
     * 프로필 조회와 달리 독립된 API라 여기서 직접 회원 존재를 검증한다.
     */
    public MemberStampListResponse getMemberStamps(Long memberId) {
        if (!memberExistenceQueryService.existsMember(memberId)) {
            log.warn("존재하지 않는 회원의 스탬프 탭 조회 시도: memberId={}", memberId);
            throw new CustomException(MemberErrorCode.MEMBER_NOT_FOUND);
        }

        List<MemberStampResponse> stamps = memberStampRepository.findMemberStampsByMemberId(memberId).stream()
                .sorted(Comparator.comparing(MemberStampQueryService::lineOrder)
                        .thenComparing(MemberStampView::getStationName))
                .map(this::toStampResponse)
                .toList();

        return new MemberStampListResponse(stamps.size(), stamps);
    }

    // LineCode enum 선언 순서가 1~9호선 → 기타 노선이라 ordinal이 곧 정렬 순서다.
    // 대표 호선이 없는 역(lineCode == null)은 맨 뒤로 보낸다.
    private static int lineOrder(MemberStampView stamp) {
        return stamp.getLineCode() == null ? Integer.MAX_VALUE : stamp.getLineCode().ordinal();
    }

    private MemberStampResponse toStampResponse(MemberStampView stamp) {
        LineSummaryResponse line = (stamp.getLineId() == null)
                ? null
                : new LineSummaryResponse(stamp.getLineId(), stamp.getLineName(), stamp.getLineCode());
        return new MemberStampResponse(stamp.getStationId(), stamp.getStationName(), line);
    }
}
