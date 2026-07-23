package com.cotato.nextstation.domain.image.service.command;

import com.cotato.nextstation.domain.image.dto.response.PresignedUrlResponse;
import com.cotato.nextstation.domain.image.enums.S3Folder;
import com.cotato.nextstation.domain.image.exception.ImageErrorCode;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ImageCommandServiceTest {

    private static final String BUCKET_NAME = "test-bucket";
    private static final String REGION = "ap-northeast-2";
    private static final Long MEMBER_ID = 1L;
    private static final Long JOURNAL_ID = 10L;

    @Mock
    private S3Presigner s3Presigner;

    private ImageCommandService imageCommandService;

    @BeforeEach
    void setUp() {
        imageCommandService = new ImageCommandService(s3Presigner, BUCKET_NAME, REGION);
    }

    private void givenPresignedUrl(String url) throws Exception {
        PresignedPutObjectRequest presignedPutObjectRequest = org.mockito.Mockito.mock(PresignedPutObjectRequest.class);
        given(presignedPutObjectRequest.url()).willReturn(URI.create(url).toURL());
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedPutObjectRequest);
    }

    @Test
    @DisplayName("PROFILE 폴더 정상 요청이면 images/uploads/profile/{memberId}/{uuid}.{ext} 형태의 key로 발급된다")
    void getPresignedUrl_profileSuccess() throws Exception {
        // given
        givenPresignedUrl("https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/uploads/profile/1/uuid.jpg?X-Amz-Signature=abc");

        // when
        PresignedUrlResponse response = imageCommandService.getPresignedUrl(S3Folder.PROFILE, MEMBER_ID, null, "profile.jpg");

        // then
        assertThat(response.presignedUrl()).contains("X-Amz-Signature");
        assertThat(response.imageUrl())
                .startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/uploads/profile/1/")
                .endsWith(".jpg");
        assertThat(response.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("JOURNAL 폴더 정상 요청이면 images/uploads/journal/{memberId}/{journalId}/{uuid}.{ext} 형태의 key로 발급된다")
    void getPresignedUrl_journalSuccess() throws Exception {
        // given
        givenPresignedUrl("https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/uploads/journal/1/10/uuid.png?X-Amz-Signature=abc");

        // when
        PresignedUrlResponse response = imageCommandService.getPresignedUrl(S3Folder.JOURNAL, MEMBER_ID, JOURNAL_ID, "photo.png");

        // then
        assertThat(response.imageUrl())
                .startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/uploads/journal/1/10/")
                .endsWith(".png");
        assertThat(response.contentType()).isEqualTo("image/png");
    }

    @ParameterizedTest
    @DisplayName("확장자에 맞는 Content-Type으로 매핑된다")
    @CsvSource({
            "photo.jpg, image/jpeg",
            "photo.JPEG, image/jpeg",
            "photo.png, image/png",
            "photo.webp, image/webp",
            "photo.gif, image/gif",
    })
    void getPresignedUrl_contentTypeMapping(String fileName, String expectedContentType) throws Exception {
        // given
        givenPresignedUrl("https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/uploads/profile/1/uuid." + expectedContentType);

        // when
        PresignedUrlResponse response = imageCommandService.getPresignedUrl(S3Folder.PROFILE, MEMBER_ID, null, fileName);

        // then
        assertThat(response.contentType()).isEqualTo(expectedContentType);
    }

    @Test
    @DisplayName("파일명에 확장자가 없으면 예외가 발생한다")
    void getPresignedUrl_noExtension() {
        assertThatThrownBy(() -> imageCommandService.getPresignedUrl(S3Folder.PROFILE, MEMBER_ID, null, "profile"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ImageErrorCode.INVALID_FILE_NAME.getMessage());
    }

    @Test
    @DisplayName("파일명이 점(.)으로 끝나면 예외가 발생한다")
    void getPresignedUrl_trailingDot() {
        assertThatThrownBy(() -> imageCommandService.getPresignedUrl(S3Folder.PROFILE, MEMBER_ID, null, "profile."))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ImageErrorCode.INVALID_FILE_NAME.getMessage());
    }

    @Test
    @DisplayName("지원하지 않는 확장자면 예외가 발생한다")
    void getPresignedUrl_unsupportedExtension() {
        assertThatThrownBy(() -> imageCommandService.getPresignedUrl(S3Folder.PROFILE, MEMBER_ID, null, "profile.bmp"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ImageErrorCode.UNSUPPORTED_FILE_EXTENSION.getMessage());
    }

    @Test
    @DisplayName("memberId 없이 PROFILE 업로드를 요청하면 예외가 발생한다")
    void getPresignedUrl_profileMissingMemberId() {
        assertThatThrownBy(() -> imageCommandService.getPresignedUrl(S3Folder.PROFILE, null, null, "profile.jpg"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ImageErrorCode.MISSING_MEMBER_ID.getMessage());
    }

    @Test
    @DisplayName("journalId 없이 JOURNAL 업로드를 요청하면 예외가 발생한다")
    void getPresignedUrl_journalMissingJournalId() {
        assertThatThrownBy(() -> imageCommandService.getPresignedUrl(S3Folder.JOURNAL, MEMBER_ID, null, "photo.jpg"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ImageErrorCode.MISSING_JOURNAL_ID.getMessage());
    }

    @Test
    @DisplayName("STATIC_PLACE 폴더는 presigned URL 발급 대상이 아니라 예외가 발생한다")
    void getPresignedUrl_staticPlaceUnsupported() {
        assertThatThrownBy(() -> imageCommandService.getPresignedUrl(S3Folder.STATIC_PLACE, MEMBER_ID, null, "place.jpg"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ImageErrorCode.UNSUPPORTED_UPLOAD_FOLDER.getMessage());
    }
}