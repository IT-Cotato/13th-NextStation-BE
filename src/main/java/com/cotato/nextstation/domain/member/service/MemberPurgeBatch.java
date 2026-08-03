package com.cotato.nextstation.domain.member.service;

import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.member.repository.MemberSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 유예 기간이 끝난 탈퇴 회원의 개인정보를 파기한다.
 * <p>
 * 행 자체를 지우지 않는 이유는 여행일지가 회원을 FK로 참조하고 있어(Journal.member),
 * 회원을 삭제하려면 일지·코스·리뷰·리뷰 사진까지 연쇄 삭제해야 하기 때문이다.
 * 파기 후 email이 비므로 같은 이메일로 재가입할 수 있게 되며, 이는 hard delete와 동일한 효과다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberPurgeBatch {

    private final MemberRepository memberRepository;
    private final MemberSocialAccountRepository memberSocialAccountRepository;

    // 매일 새벽 4시 30분. 같은 시간대의 EmailVerificationCleaner(4시)와 겹치지 않게 띄운다.
    // 하루 한 번이라 실제 파기는 유예 만료 시점에서 최대 하루 늦게 일어난다.
    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void purgeExpiredWithdrawals() {
        LocalDateTime threshold = LocalDateTime.now().minus(Member.WITHDRAWAL_GRACE_PERIOD);
        List<Member> targets = memberRepository
                .findAllByStatusAndDeletedAtBeforeAndPurgedAtIsNull(MemberStatus.WITHDRAWN, threshold);

        if (targets.isEmpty()) {
            log.info("파기 대상 탈퇴 회원 없음: threshold={}", threshold);
            return;
        }

        for (Member member : targets) {
            // 소셜 연결을 끊어야 같은 카카오 계정으로 다시 가입할 수 있다.
            int deletedSocialAccounts = memberSocialAccountRepository.deleteByMemberId(member.getId());
            member.purge();
            log.info("탈퇴 회원 개인정보 파기: memberId={}, deletedSocialAccounts={}", member.getId(), deletedSocialAccounts);
        }

        log.info("탈퇴 회원 파기 완료: count={}, threshold={}", targets.size(), threshold);
    }
}