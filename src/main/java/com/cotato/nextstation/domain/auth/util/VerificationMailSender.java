package com.cotato.nextstation.domain.auth.util;

import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class VerificationMailSender {

    private static final String SUBJECT = "[환승여행] 이메일 인증번호 안내";

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final long codeExpirationMillis;

    public VerificationMailSender(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String fromAddress,
            @Value("${spring.mail.auth-code-expiration-millis}") long codeExpirationMillis
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.codeExpirationMillis = codeExpirationMillis;
    }

    public void sendVerificationCode(String to, String code) {
        long expirationMinutes = Duration.ofMillis(codeExpirationMillis).toMinutes();

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(SUBJECT);
            helper.setText(buildHtmlContent(code, expirationMinutes), true);

            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException e) {
            log.warn("이메일 발송 실패: to={}", to, e);
            throw new CustomException(GlobalErrorCode.EXTERNAL_API_ERROR);
        }
    }

    // 이메일 템플릿
    private String buildHtmlContent(String code, long expirationMinutes) {
        return """
                <div style="max-width:480px;margin:0 auto;padding:40px 32px;font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;background-color:#ffffff;border:1px solid #e5e7eb;border-radius:16px;">
                  <p style="margin:0 0 4px;font-size:13px;font-weight:600;color:#DB868D;letter-spacing:0.5px;">환승여행</p>
                  <h1 style="margin:0 0 8px;font-size:20px;color:#111827;">이메일 인증번호 안내</h1>
                  <p style="margin:0 0 28px;font-size:14px;color:#6b7280;line-height:1.5;">아래 인증번호를 입력창에 입력해주세요.</p>
                  <div style="background-color:#f3f4f6;border-radius:12px;padding:24px;text-align:center;">
                    <span style="font-size:32px;font-weight:700;letter-spacing:8px;color:#DB868D;">%s</span>
                    <div style="margin-top:12px;font-size:13px;color:#9ca3af;">발급 후 %d분간 유효합니다.</div>
                  </div>
                </div>
                """.formatted(code, expirationMinutes);
    }
}