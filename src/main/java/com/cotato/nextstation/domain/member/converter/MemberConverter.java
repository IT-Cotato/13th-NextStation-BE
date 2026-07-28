package com.cotato.nextstation.domain.member.converter;

import com.cotato.nextstation.domain.member.dto.response.MemberProfileResponse;
import com.cotato.nextstation.domain.member.entity.Member;
import org.springframework.stereotype.Component;

@Component
public class MemberConverter {

    public MemberProfileResponse toProfileResponse(Member member) {
        return new MemberProfileResponse(member.getId(), member.getNickname(), member.getProfileImageUrl());
    }
}