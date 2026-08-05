package com.cotato.nextstation.domain.member.service;

import com.cotato.nextstation.domain.auth.repository.RefreshSessionRepository;
import com.cotato.nextstation.domain.member.service.command.MemberCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 탈퇴의 DB 처리와 Redis 세션 정리를 순서대로 묶는다.
 * <p>
 * 트랜잭션을 열지 않는다.
 * 세션 삭제가 트랜잭션 안에서 일어나면 DB가 롤백돼도 Redis는 되돌아오지 않아 "로그아웃은 됐는데 탈퇴는 안 된" 상태가 남는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberWithdrawService {

    private final MemberCommandService memberCommandService;
    private final RefreshSessionRepository refreshSessionRepository;

    public void withdraw(Long memberId) {
        memberCommandService.withdraw(memberId);

        try {
            int deletedSessions = refreshSessionRepository.deleteAllOf(memberId);
            log.info("탈퇴 회원 세션 정리 완료: memberId={}, deletedSessions={}", memberId, deletedSessions);
        } catch (Exception e) {
            log.error("탈퇴 회원 세션 정리 실패 - 탈퇴 자체는 완료됨: memberId={}", memberId, e);
        }
    }
}