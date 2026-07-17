package com.cotato.nextstation.domain.auth.service;

import com.cotato.nextstation.domain.auth.entity.VerificationType;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.util.VerificationMailSender;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailVerificationCommandService {

    private final MemberRepository memberRepository;
    private final EmailVerificationWriter emailVerificationWriter;
    private final VerificationMailSender verificationMailSender;
    private final long codeExpirationMillis;

    public EmailVerificationCommandService(
            MemberRepository memberRepository,
            EmailVerificationWriter emailVerificationWriter,
            VerificationMailSender verificationMailSender,
            @Value("${spring.mail.auth-code-expiration-millis}") long codeExpirationMillis
    ) {
        this.memberRepository = memberRepository;
        this.emailVerificationWriter = emailVerificationWriter;
        this.verificationMailSender = verificationMailSender;
        this.codeExpirationMillis = codeExpirationMillis;
    }

    // 회원가입 인증
    public void sendSignupVerificationCode(String email) {
        log.info("이메일 인증번호 발송 요청: type={}, email={}", VerificationType.SIGNUP, email);

        validateNotAlreadyRegistered(email);

        // DB/Redis 쓰기는 EmailVerificationWriter의 트랜잭션 안에서 커밋까지 완료된다.
        // 메일 발송(외부 API 호출)은 그 트랜잭션이 끝난 뒤 여기서 별도로 수행되므로
        // SMTP 응답 지연이 DB 커넥션을 점유하지 않는다.
        String code = emailVerificationWriter.issue(email, VerificationType.SIGNUP, codeExpirationMillis);

        verificationMailSender.sendVerificationCode(email, code);
        log.info("이메일 인증번호 발송 완료: type={}, email={}", VerificationType.SIGNUP, email);
    }

    // 이미 가입한 이메일인지 확인
    private void validateNotAlreadyRegistered(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }
    }
}