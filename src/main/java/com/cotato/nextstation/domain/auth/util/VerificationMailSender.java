package com.cotato.nextstation.domain.auth.util;

import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationMailSender {

    private static final String SUBJECT = "[환승여행] 이메일 인증코드 안내";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private final String fromAddress;

    @Value("${spring.mail.auth-code-expiration-millis}")
    private final long codeExpirationMillis;

    public void sendVerificationCode(String to, String code) {
        long expirationMinutes = Duration.ofMillis(codeExpirationMillis).toMinutes();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(SUBJECT);
        message.setText("인증코드: %s\n%d분 이내에 입력해주세요.".formatted(code, expirationMinutes));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("이메일 발송 실패: to={}", to, e);
            throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);
        }
    }
}