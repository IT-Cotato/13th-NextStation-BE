package com.cotato.nextstation.domain.auth.service;

import com.cotato.nextstation.domain.auth.entity.VerificationStatus;
import com.cotato.nextstation.domain.auth.repository.EmailVerificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EmailVerificationCleanerTest {

    @InjectMocks
    private EmailVerificationCleaner emailVerificationCleaner;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Test
    @DisplayName("EXPIRED/FAILED 상태이면서 7일 지난 인증번호 기록을 삭제 요청한다")
    void cleanupOldVerifications_success() {
        // given
        ArgumentCaptor<List<VerificationStatus>> statusesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        given(emailVerificationRepository.deleteByStatusInAndCreatedAtBefore(statusesCaptor.capture(), thresholdCaptor.capture()))
                .willReturn(3);

        // when
        emailVerificationCleaner.cleanupOldVerifications();

        // then
        assertThat(statusesCaptor.getValue()).containsExactlyInAnyOrder(VerificationStatus.EXPIRED, VerificationStatus.FAILED);
        assertThat(thresholdCaptor.getValue()).isCloseTo(LocalDateTime.now().minusDays(7), within(2, ChronoUnit.SECONDS));
    }
}