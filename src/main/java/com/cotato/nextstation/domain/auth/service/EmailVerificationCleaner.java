package com.cotato.nextstation.domain.auth.service;

import com.cotato.nextstation.domain.auth.entity.VerificationStatus;
import com.cotato.nextstation.domain.auth.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationCleaner {

    private static final int RETENTION_DAYS = 7;
    private static final List<VerificationStatus> CLEANUP_TARGET_STATUSES =
            List.of(VerificationStatus.EXPIRED, VerificationStatus.FAILED);

    private final EmailVerificationRepository emailVerificationRepository;

    // 매일 새벽 4시, 만료/실패한 지 7일 지난 인증번호 기록을 삭제한다.
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupOldVerifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int deletedCount = emailVerificationRepository.deleteByStatusInAndCreatedAtBefore(CLEANUP_TARGET_STATUSES, threshold);
        log.info("만료/실패한 인증번호 삭제 완료: deletedCount={}, threshold={}", deletedCount, threshold);
    }
}