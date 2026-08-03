package com.cotato.nextstation.domain.stamp.service.query;

import com.cotato.nextstation.domain.journal.repository.JournalRepository;
import com.cotato.nextstation.domain.stamp.converter.MemberStampConverter;
import com.cotato.nextstation.domain.stamp.dto.response.MyStampDetailResponse;
import com.cotato.nextstation.domain.stamp.dto.response.MyStampListResponse;
import com.cotato.nextstation.domain.stamp.entity.MemberStamp;
import com.cotato.nextstation.domain.stamp.exception.StampErrorCode;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository.MyStampDetailView;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository.MyStampView;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberStampQueryService {

    private final MemberStampRepository memberStampRepository;
    private final JournalRepository journalRepository;
    private final MemberStampConverter memberStampConverter;

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

    // 내 스탬프 목록. 역별 중복 제거는 리포지토리 쿼리(DISTINCT)가 처리하고,
    // 여기서는 1호선 → 9호선 순으로 정렬만 한다(대표 호선 없는 역은 맨 뒤).
    public MyStampListResponse getMyStamps(Long memberId) {
        List<MyStampView> stamps = memberStampRepository.findMyStampsByMemberId(memberId);

        List<MyStampView> sorted = stamps.stream()
                .sorted(Comparator.comparing(MemberStampQueryService::lineOrder))
                .toList();

        return memberStampConverter.toMyStampListResponse(sorted);
    }

    // 내 스탬프 상세. 해당 역에서 최초로 완료한 스탬프 기준으로 역/노선/획득일과
    // 그 스탬프에 연결된 여행일지 id(없으면 null)를 보여준다.
    public MyStampDetailResponse getMyStampDetail(Long memberId, Long stationId) {
        MyStampDetailView detail = memberStampRepository
                .findEarliestStampByMemberIdAndStationId(memberId, stationId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new CustomException(StampErrorCode.MEMBER_STAMP_NOT_FOUND));

        return memberStampConverter.toMyStampDetailResponse(detail);
    }

    private static int lineOrder(MyStampView stamp) {
        return stamp.getLineCode() == null ? Integer.MAX_VALUE : stamp.getLineCode().ordinal();
    }
}
