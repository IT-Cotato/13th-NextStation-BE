package com.cotato.nextstation.domain.member.service.command;

import com.cotato.nextstation.domain.image.service.command.ImageCommandService;
import com.cotato.nextstation.domain.member.converter.MemberConverter;
import com.cotato.nextstation.domain.member.dto.response.MemberProfileResponse;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

        // 이전 S3 이미지 삭제는 트랜잭션 커밋 이후로 미룬다. saveAndFlush 뒤라도 메서드가 끝나기 전엔 커밋 전이라,
        // 여기서 바로 호출하면 S3 오류 하나로 이미 반영된 닉네임/이미지 변경까지 롤백되고 커넥션도 그만큼 오래 붙잡힌다.
        if (previousProfileImageUrl != null && !previousProfileImageUrl.equals(member.getProfileImageUrl())) {
            scheduleOldImageDeletion(previousProfileImageUrl, memberId);
        }

        log.info("프로필 수정 완료: memberId={}", memberId);
        return memberConverter.toProfileResponse(member);
    }

    private void scheduleOldImageDeletion(String imageUrl, Long memberId) {
        if (!profileImageUrlValidator.isOwnS3Object(imageUrl, memberId)) {
            log.info("외부 CDN 프로필 이미지라 S3 삭제를 건너뛴다: memberId={}", memberId);
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 트랜잭션 밖에서 호출된 경우(예: 테스트)를 위한 폴백 — 정상 흐름에서는 항상 @Transactional 안이라 아래 분기를 탄다
            deleteOldImage(imageUrl, memberId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteOldImage(imageUrl, memberId);
            }
        });
    }

    private void deleteOldImage(String imageUrl, Long memberId) {
        try {
            imageCommandService.deleteImage(imageUrl, memberId);
        } catch (Exception e) {
            // 커밋 이후 실행이라 실패해도 프로필 수정 자체는 이미 성공한 상태 — 로그만 남기고 무시한다 (S3에 orphan 파일만 남음)
            log.warn("이전 프로필 이미지 삭제 실패(무시): memberId={}", memberId, e);
        }
    }

    private void applyProfileImageUrl(Member member, String profileImageUrl) {
        if (profileImageUrl.isBlank()) {
            member.changeProfileImageUrl(null);
            return;
        }
        profileImageUrlValidator.validate(profileImageUrl, member.getId());
        member.changeProfileImageUrl(profileImageUrl);
    }

    /**
     * 탈퇴 - soft delete로 status/deletedAt만 기록한다.
     * 유예 기간(7일) 안에 계정을 복구할 수 있으므로 개인정보는 파기하지 않는다.
     * TODO: 유예가 지난 회원의 개인정보 파기는 별도 배치가 담당한다.
     */
    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 회원의 탈퇴 시도: memberId={}", memberId);
                    return new CustomException(MemberErrorCode.MEMBER_NOT_FOUND);
                });

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            log.info("이미 탈퇴한 회원의 탈퇴 재요청 - 무시: memberId={}", memberId);
            return;
        }

        MemberStatus previousStatus = member.getStatus();
        member.withdraw();
        log.info("회원 탈퇴 처리 완료: memberId={}, previousStatus={}", memberId, previousStatus);
    }

    /**
     * 유예 기간 내 계정 복구. 복구 후의 상태를 반환한다.
     * 트랜잭션이 없는 로그인 경로(카카오/로컬)에서도 독립적으로 커밋되도록 서비스로 분리해 둔다.
     */
    @Transactional
    public MemberStatus restore(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 회원의 복구 시도: memberId={}", memberId);
                    return new CustomException(MemberErrorCode.MEMBER_NOT_FOUND);
                });

        if (!member.isRestorable()) {
            log.warn("복구할 수 없는 회원의 복구 시도: memberId={}, status={}, deletedAt={}",
                    memberId, member.getStatus(), member.getDeletedAt());
            return member.getStatus();
        }

        member.restore();
        log.info("탈퇴 유예 기간 내 계정 복구: memberId={}, restoredStatus={}", memberId, member.getStatus());
        return member.getStatus();
    }
}