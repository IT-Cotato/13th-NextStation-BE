package com.cotato.nextstation.domain.member.util;

import com.cotato.nextstation.domain.image.enums.S3Folder;
import com.cotato.nextstation.domain.member.exception.MemberErrorCode;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Slf4j
@Component
public class ProfileImageUrlValidator {

    private final String expectedHost;
    private final List<String> allowedExternalHostSuffixes;

    public ProfileImageUrlValidator(@Value("${aws.s3.bucket-name}") String bucketName,
                                     @Value("${spring.cloud.aws.region.static}") String region,
                                     @Value("${member.profile-image.allowed-host-suffixes}") List<String> allowedExternalHostSuffixes) {
        this.expectedHost = "%s.s3.%s.amazonaws.com".formatted(bucketName, region);
        this.allowedExternalHostSuffixes = List.copyOf(allowedExternalHostSuffixes);
    }

    /**
     * 프로필 이미지로 저장할 수 있는 URL인지 검증한다. (임의 외부 URL/XSS 스킴 차단)
     * 허용 대상은 두 가지다.
     * <ul>
     *   <li>본인 presigned URL로 발급받은 S3 프로필 경로
     *   <li>소셜 로그인 응답으로 서버가 직접 내려준 외부 CDN 이미지 (카카오 프로필 이미지)
     * </ul>
     */
    public void validate(String profileImageUrl, Long memberId) {
        URI uri;
        try {
            uri = new URI(profileImageUrl);
        } catch (URISyntaxException e) {
            log.warn("파싱할 수 없는 프로필 이미지 URL: memberId={}", memberId);
            throw new CustomException(MemberErrorCode.INVALID_PROFILE_IMAGE_URL);
        }

        boolean isAllowed = "https".equalsIgnoreCase(uri.getScheme())
                && (isOwnS3Object(uri, memberId) || isAllowedExternalHost(uri));

        if (!isAllowed) {
            log.warn("허용되지 않은 프로필 이미지 URL: memberId={}, scheme={}, host={}",
                    memberId, uri.getScheme(), uri.getHost());
            throw new CustomException(MemberErrorCode.INVALID_PROFILE_IMAGE_URL);
        }
    }

    // 버킷에 올라간 이미지인지 여부, 외부 CDN 이미지는 S3 삭제 대상이 아니라 호출부에서 구분이 필요하다.
    public boolean isOwnS3Object(String profileImageUrl, Long memberId) {
        try {
            return isOwnS3Object(new URI(profileImageUrl), memberId);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private boolean isOwnS3Object(URI uri, Long memberId) {
        String expectedPathPrefix = "/%s/%d/".formatted(S3Folder.PROFILE.getPath(), memberId);
        return expectedHost.equals(uri.getHost())
                && uri.getRawPath() != null
                && uri.getRawPath().startsWith(expectedPathPrefix);
    }

    // 카카오는 계정/업로드 시점에 따라 k./img1./t1. 등 서브도메인이 갈리므로 호스트를 고정하지 않고 도메인 접미사로 판정한다.
    // 접미사는 반드시 점으로 시작해야 한다 - "kakaocdn.net"으로 두면 "evilkakaocdn.net"까지 통과한다.
    private boolean isAllowedExternalHost(URI uri) {
        String host = uri.getHost();
        return host != null && allowedExternalHostSuffixes.stream().anyMatch(host::endsWith);
    }
}