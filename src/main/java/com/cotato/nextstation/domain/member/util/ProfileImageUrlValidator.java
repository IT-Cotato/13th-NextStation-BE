package com.cotato.nextstation.domain.member.util;

import com.cotato.nextstation.domain.image.enums.S3Folder;
import com.cotato.nextstation.domain.member.exception.MemberErrorCode;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class ProfileImageUrlValidator {

    private final String expectedHost;

    public ProfileImageUrlValidator(@Value("${aws.s3.bucket-name}") String bucketName,
                                     @Value("${spring.cloud.aws.region.static}") String region) {
        this.expectedHost = "%s.s3.%s.amazonaws.com".formatted(bucketName, region);
    }

    // 본인 presigned URL로 발급받은 S3 프로필 이미지 경로인지 검증 (임의 외부 URL/XSS 스킴 차단)
    public void validate(String profileImageUrl, Long memberId) {
        URI uri;
        try {
            uri = new URI(profileImageUrl);
        } catch (URISyntaxException e) {
            log.warn("파싱할 수 없는 프로필 이미지 URL: memberId={}", memberId);
            throw new CustomException(MemberErrorCode.INVALID_PROFILE_IMAGE_URL);
        }

        String expectedPathPrefix = "/%s/%d/".formatted(S3Folder.PROFILE.getPath(), memberId);
        boolean isAllowed = "https".equalsIgnoreCase(uri.getScheme())
                && expectedHost.equals(uri.getHost())
                && uri.getRawPath() != null
                && uri.getRawPath().startsWith(expectedPathPrefix);

        if (!isAllowed) {
            // 거부된 URL 원문은 로그에 남기지 않는다 (사용자 입력값 그대로 기록하지 않기 위함)
            log.warn("허용되지 않은 프로필 이미지 URL: memberId={}", memberId);
            throw new CustomException(MemberErrorCode.INVALID_PROFILE_IMAGE_URL);
        }
    }
}