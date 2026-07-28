package com.cotato.nextstation.domain.member.service.query;

import com.cotato.nextstation.domain.member.converter.MemberConverter;
import com.cotato.nextstation.domain.member.dto.response.MemberProfileResponse;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.exception.MemberErrorCode;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberRepository memberRepository;
    private final MemberConverter memberConverter;

    // 내 프로필(닉네임/프로필 이미지) 조회
    public MemberProfileResponse getMyProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 멤버의 프로필 조회 시도: memberId={}", memberId);
                    return new CustomException(MemberErrorCode.MEMBER_NOT_FOUND);
                });
        return memberConverter.toProfileResponse(member);
    }
}