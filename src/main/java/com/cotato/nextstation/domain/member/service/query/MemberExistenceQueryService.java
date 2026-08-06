package com.cotato.nextstation.domain.member.service.query;

import com.cotato.nextstation.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 존재 여부만 확인하는 조회 전용 서비스. 다른 도메인이 회원 존재를 물어볼 때 이 서비스를 호출한다.
 * MemberQueryService가 다른 회원 프로필 조회에서 CourseQueryService·MemberStampQueryService를
 * 주입받고 있어, 그 두 서비스가 다시 MemberQueryService를 주입받으면 순환 참조가 된다.
 * 그래서 존재 여부 확인만 담는 서비스를 따로 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberExistenceQueryService {

    private final MemberRepository memberRepository;

    public boolean existsMember(Long memberId) {
        return memberRepository.existsById(memberId);
    }
}
