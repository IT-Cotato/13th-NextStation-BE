package com.cotato.nextstation.domain.member.service.command;

import com.cotato.nextstation.domain.image.service.command.ImageCommandService;
import com.cotato.nextstation.domain.member.converter.MemberConverter;
import com.cotato.nextstation.domain.member.dto.response.MemberProfileResponse;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.exception.MemberErrorCode;
import com.cotato.nextstation.domain.member.exception.NicknameErrorCode;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.member.util.NicknameValidator;
import com.cotato.nextstation.domain.member.util.ProfileImageUrlValidator;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberCommandService {

    private final MemberRepository memberRepository;
    private final MemberConverter memberConverter;
    private final NicknameValidator nicknameValidator;
    private final ProfileImageUrlValidator profileImageUrlValidator;
    private final ImageCommandService imageCommandService;

    // nickname/profileImageUrl 중 요청에 넘어온 필드만 부분 수정한다 (null이면 미변경, profileImageUrl은 빈 문자열이면 제거)
    @Transactional
    public MemberProfileResponse updateMyProfile(Long memberId, String nickname, String profileImageUrl) {
        log.info("프로필 수정 요청: memberId={}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 멤버의 프로필 수정 시도: memberId={}", memberId);
                    return new CustomException(MemberErrorCode.MEMBER_NOT_FOUND);
                });

        if (nickname != null && !nickname.equals(member.getNickname())) {
            nicknameValidator.validate(nickname);
            member.changeNickname(nickname);
        }

        String previousProfileImageUrl = member.getProfileImageUrl();
        if (profileImageUrl != null) {
            applyProfileImageUrl(member, profileImageUrl);
        }

        try {
            memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            // 위 existsByNickname 조회 이후 동시에 같은 닉네임으로 들어온 요청이 먼저 커밋된 경우 (레이스 컨디션)
            log.warn("닉네임 중복 저장 시도(레이스 컨디션): nickname={}", nickname);
            throw new CustomException(NicknameErrorCode.DUPLICATE_NICKNAME);
        }

        // DB 반영이 끝난 뒤에 이전 S3 이미지를 정리한다. 순서를 바꾸면 저장 실패 시 이미지만 먼저 지워져 깨진 링크가 남는다.
        if (previousProfileImageUrl != null && !previousProfileImageUrl.equals(member.getProfileImageUrl())) {
            imageCommandService.deleteImage(previousProfileImageUrl, memberId);
        }

        log.info("프로필 수정 완료: memberId={}", memberId);
        return memberConverter.toProfileResponse(member);
    }

    private void applyProfileImageUrl(Member member, String profileImageUrl) {
        if (profileImageUrl.isBlank()) {
            member.changeProfileImageUrl(null);
            return;
        }
        profileImageUrlValidator.validate(profileImageUrl, member.getId());
        member.changeProfileImageUrl(profileImageUrl);
    }
}