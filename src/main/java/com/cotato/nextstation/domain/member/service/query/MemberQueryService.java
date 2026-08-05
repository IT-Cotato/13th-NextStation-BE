package com.cotato.nextstation.domain.member.service.query;

import com.cotato.nextstation.domain.member.converter.MemberConverter;
import com.cotato.nextstation.domain.member.dto.response.AccountInfoResponse;
import com.cotato.nextstation.domain.member.dto.response.MemberProfileResponse;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberSocialAccount;
import com.cotato.nextstation.domain.member.exception.MemberErrorCode;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.member.repository.MemberSocialAccountRepository;
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
    private final MemberSocialAccountRepository memberSocialAccountRepository;
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

    // 계정 정보(가입 경로 + 가입한 이메일) 조회
    public AccountInfoResponse getMyAccountInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 멤버의 계정 정보 조회 시도: memberId={}", memberId);
                    return new CustomException(MemberErrorCode.MEMBER_NOT_FOUND);
                });

        MemberSocialAccount socialAccount = memberSocialAccountRepository.findFirstByMemberId(memberId)
                .orElse(null);
        log.info("계정 정보 조회: memberId={}, social={}", memberId, socialAccount != null);

        return memberConverter.toAccountInfoResponse(member, socialAccount);
    }
}