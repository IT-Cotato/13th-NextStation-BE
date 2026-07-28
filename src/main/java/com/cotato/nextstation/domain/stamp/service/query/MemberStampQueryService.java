package com.cotato.nextstation.domain.stamp.service.query;

import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository;
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

    // 넘긴 코스들 중 회원이 완료한 코스 id 집합. 목록에서 카드별 완료 여부를 판단하는 데 쓴다.
    public Set<Long> getCompletedCourseIds(Long memberId, List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(memberStampRepository.findCompletedCourseIds(memberId, courseIds));
    }
}
