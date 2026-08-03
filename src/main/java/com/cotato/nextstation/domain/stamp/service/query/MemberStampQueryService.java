package com.cotato.nextstation.domain.stamp.service.query;

import com.cotato.nextstation.domain.journal.repository.JournalRepository;
import com.cotato.nextstation.domain.member.exception.MemberErrorCode;
import com.cotato.nextstation.domain.member.service.query.MemberExistenceQueryService;
import com.cotato.nextstation.domain.stamp.dto.response.MemberStampListResponse;
import com.cotato.nextstation.domain.stamp.entity.MemberStamp;
import com.cotato.nextstation.domain.stamp.exception.StampErrorCode;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.service.query.StationQueryService;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 스탬프 조회 전용 서비스. 다른 도메인이 여행 완료 여부를 물어볼 때 이 서비스를 호출한다.
 * StampCourseQueryService가 CourseQueryService를 주입받고 있어, 코스 목록이 그 서비스를
 * 다시 호출하면 순환 참조가 된다. 그래서 완료 여부 조회만 담는 서비스를 따로 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberStampQueryService {

    private final MemberStampRepository memberStampRepository;
    private final JournalRepository journalRepository;
    private final MemberExistenceQueryService memberExistenceQueryService;
    private final StationQueryService stationQueryService;

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

    // 다른 회원의 스탬프 탭. 방문한 역을 최근 방문순으로 조회한다(역 하나당 스탬프 1개).
    // 프로필 조회와 달리 독립된 API라 여기서 직접 회원 존재를 검증한다.
    public MemberStampListResponse getMemberStamps(Long memberId) {
        if (!memberExistenceQueryService.existsMember(memberId)) {
            throw new CustomException(MemberErrorCode.MEMBER_NOT_FOUND);
        }
        List<Long> stationIds = memberStampRepository.findVisitedStationIdsOrderByLastVisitedDesc(memberId);
        List<StationSummaryResponse> stamps = stationQueryService.getStationSummaries(stationIds);
        return new MemberStampListResponse(stamps.size(), stamps);
    }
}
