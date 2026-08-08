package com.cotato.nextstation.domain.member.converter;

import com.cotato.nextstation.domain.member.dto.response.AccountInfoResponse;
import com.cotato.nextstation.domain.member.dto.response.MemberProfileResponse;
import com.cotato.nextstation.domain.member.dto.response.OtherMemberProfileResponse;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberSocialAccount;
import org.springframework.stereotype.Component;

@Component
public class MemberConverter {

    public MemberProfileResponse toProfileResponse(Member member) {
        return new MemberProfileResponse(member.getId(), member.getNickname(), member.getProfileImageUrl());
    }

    // socialAccount가 null이면 이메일/비밀번호로 가입한 로컬 계정
    public AccountInfoResponse toAccountInfoResponse(Member member, MemberSocialAccount socialAccount) {
        String provider = socialAccount == null ? "LOCAL" : socialAccount.getProvider().name();
        return new AccountInfoResponse(provider, member.getEmail());
    }
      
    public OtherMemberProfileResponse toOtherProfileResponse(Member member, long stampCount, long publicCourseCount) {
        return new OtherMemberProfileResponse(
                member.getId(), member.getNickname(), member.getProfileImageUrl(), stampCount, publicCourseCount);
    }
}