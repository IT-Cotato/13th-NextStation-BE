package com.cotato.nextstation.domain.auth.service.command;

import com.cotato.nextstation.domain.auth.entity.EmailVerification;
import com.cotato.nextstation.domain.auth.entity.VerificationStatus;
import com.cotato.nextstation.domain.auth.entity.VerificationType;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.repository.EmailVerificationRepository;
import com.cotato.nextstation.domain.auth.util.EmailMasker;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetCommandService {

    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(noRollbackFor = CustomException.class)
    public void resetPassword(String email, String code, String newPassword, String newPasswordConfirm) {
        log.info("비밀번호 재설정 요청: email={}", EmailMasker.mask(email));

        validatePasswordConfirmation(newPassword, newPasswordConfirm);

        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(email, VerificationType.PASSWORD_RESET, VerificationStatus.VERIFIED)
                .orElseThrow(() -> {
                    log.warn("인증 완료 내역 없이 비밀번호 재설정 시도: email={}", EmailMasker.mask(email));
                    return new CustomException(AuthErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
                });

        // confirm 이후 비밀번호 입력까지 시간이 걸릴 수 있으므로, 재설정 시점에 코드 일치/만료 여부를 다시 확인한다.
        if (!verification.getVerificationCode().equals(code)) {
            log.warn("비밀번호 재설정 시 인증번호 불일치: email={}", EmailMasker.mask(email));
            throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
        }
        if (verification.isExpired()) {
            log.warn("만료된 인증번호로 비밀번호 재설정 시도: email={}", EmailMasker.mask(email));
            verification.expire();
            throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 회원의 비밀번호 재설정 시도: email={}", EmailMasker.mask(email));
                    return new CustomException(AuthErrorCode.MEMBER_NOT_FOUND);
                });

        member.changePassword(passwordEncoder.encode(newPassword));
        verification.expire(); // 검증 완료된 인증번호는 재사용 방지를 위해 즉시 만료 처리

        log.info("비밀번호 재설정 완료: memberId={}", member.getId());
    }

    // 새 비밀번호와 새 비밀번호 확인 일치 여부 확인
    private void validatePasswordConfirmation(String newPassword, String newPasswordConfirm) {
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new CustomException(AuthErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }
    }
}