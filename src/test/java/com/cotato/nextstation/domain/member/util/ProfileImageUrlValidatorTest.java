package com.cotato.nextstation.domain.member.util;

import com.cotato.nextstation.domain.member.exception.MemberErrorCode;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileImageUrlValidatorTest {

    private static final Long MEMBER_ID = 1L;
    private static final String OWN_S3_URL =
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/uploads/profile/1/photo.jpg";

    private final ProfileImageUrlValidator validator =
            new ProfileImageUrlValidator("test-bucket", "ap-northeast-2", List.of(".kakaocdn.net"));

    @Test
    @DisplayName("본인 presigned URL로 올린 S3 프로필 경로는 통과한다")
    void validate_ownS3Object() {
        assertThatCode(() -> validator.validate(OWN_S3_URL, MEMBER_ID)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://k.kakaocdn.net/dn/abc/def/ghi/img_640x640.jpg",
            "https://img1.kakaocdn.net/dn/abc/def/ghi/img_110x110.jpg",
            "https://t1.kakaocdn.net/account_images/default_profile.jpeg"
    })
    @DisplayName("카카오 CDN 이미지는 서브도메인이 달라도 통과한다")
    void validate_kakaoCdnImage(String kakaoImageUrl) {
        assertThatCode(() -> validator.validate(kakaoImageUrl, MEMBER_ID)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://evil.com/xss.svg",
            // 허용 도메인으로 시작하기만 하는 외부 도메인
            "https://k.kakaocdn.net.evil.com/img_640x640.jpg",
            // 허용 도메인을 접미사로 붙인 외부 도메인 (점으로 시작하는 접미사여야 막힌다)
            "https://evilkakaocdn.net/img_640x640.jpg",
            // 다른 회원의 프로필 경로
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/uploads/profile/2/photo.jpg",
            // 우리 버킷이지만 프로필 폴더가 아닌 경로
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/static/places/1/1.jpg",
            // https가 아닌 스킴
            "http://k.kakaocdn.net/dn/abc/def/ghi/img_640x640.jpg",
            "javascript:alert(1)"
    })
    @DisplayName("허용 대상이 아닌 URL은 거부한다")
    void validate_notAllowed(String profileImageUrl) {
        assertThatThrownBy(() -> validator.validate(profileImageUrl, MEMBER_ID))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(MemberErrorCode.INVALID_PROFILE_IMAGE_URL.getMessage());
    }

    @Test
    @DisplayName("파싱할 수 없는 URL은 거부한다")
    void validate_malformedUrl() {
        assertThatThrownBy(() -> validator.validate("h ttp://not a url", MEMBER_ID))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(MemberErrorCode.INVALID_PROFILE_IMAGE_URL.getMessage());
    }

    @Test
    @DisplayName("isOwnS3Object는 우리 버킷의 본인 경로에만 true를 반환한다")
    void isOwnS3Object() {
        assertThat(validator.isOwnS3Object(OWN_S3_URL, MEMBER_ID)).isTrue();
        assertThat(validator.isOwnS3Object("https://k.kakaocdn.net/dn/abc/def/ghi/img_640x640.jpg", MEMBER_ID)).isFalse();
        assertThat(validator.isOwnS3Object("h ttp://not a url", MEMBER_ID)).isFalse();
    }
}