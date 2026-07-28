package com.cotato.nextstation.domain.image.service.command;

import com.cotato.nextstation.domain.image.dto.response.PresignedUrlResponse;
import com.cotato.nextstation.domain.image.enums.S3Folder;
import com.cotato.nextstation.domain.image.exception.ImageErrorCode;
import com.cotato.nextstation.domain.journal.entity.Journal;
import com.cotato.nextstation.domain.journal.repository.JournalRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ImageCommandService {

    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(10);

    private final JournalRepository journalRepository;
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final String bucketName;
    private final String region;

    public ImageCommandService(S3Presigner s3Presigner,
                               S3Client s3Client,
                               JournalRepository journalRepository,
                               @Value("${aws.s3.bucket-name}") String bucketName,
                                @Value("${spring.cloud.aws.region.static}") String region) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
        this.journalRepository = journalRepository;
        this.bucketName = bucketName;
        this.region = region;
    }

    // Presigned URL 생성
    public PresignedUrlResponse getPresignedUrl(S3Folder folder, Long memberId, Long journalId, String fileName) {
        validateJournalOwnership(folder, memberId, journalId);

        String extension = getExtension(fileName);
        String contentType = mapContentType(extension);
        String key = createS3Key(folder, memberId, journalId, extension);

        // 전체 URL 생성
        String imageUrl = "https://%s.s3.%s.amazonaws.com/%s".formatted(bucketName, region, key);

        // S3에 업로드할 요청 정보 설정
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        // Presigned 요청 설정 (유효기간 10분)
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_EXPIRATION)
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return new PresignedUrlResponse(presignedRequest.url().toString(), imageUrl, contentType);
    }

    // 다중 Presigned URL 생성
    public List<PresignedUrlResponse> getPresignedUrls(
            S3Folder folder, Long memberId, Long journalId, List<String> fileNames) {
        if (folder != S3Folder.JOURNAL) {
            throw new CustomException(ImageErrorCode.PROFILE_NOT_ALLOWED_IN_BATCH);
        }
        validateJournalOwnership(folder, memberId, journalId);

        return fileNames.stream()
                .map(fileName -> getPresignedUrl(folder, memberId, journalId, fileName))
                .toList();
    }

    // 이미지 삭제
    public void deleteImage(String imageUrl, Long memberId) {
        String key = extractKeyFromImageUrl(imageUrl);
        validateOwnership(key, memberId);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(deleteRequest);
        log.info("S3 이미지 삭제 완료: key={}", key);
    }

    private String extractKeyFromImageUrl(String imageUrl) {
        String prefix = "https://%s.s3.%s.amazonaws.com/".formatted(bucketName, region);
        return imageUrl.substring(prefix.length());
    }

    // key 경로(images/uploads/{folder}/{memberId}/...)에 인증된 memberId가 포함돼 있는지 확인
    private void validateOwnership(String key, Long memberId) {
        boolean owned = key.contains("/" + memberId + "/");
        if (!owned) {
            throw new CustomException(ImageErrorCode.IMAGE_ACCESS_DENIED);
        }
    }

    /** S3 객체 키 생성
     * PROFILE: images/uploads/profile/{memberId}/{uuid}.{ext}
     * JOURNAL: images/uploads/journal/{memberId}/{journalId}/{uuid}.{ext}
     * STATIC_PLACE: presigned URL 발급 대상이 아님
     */
    private String createS3Key(S3Folder folder, Long memberId, Long journalId, String extension) {

        String uuid = UUID.randomUUID().toString();

        return switch (folder) {
            case PROFILE -> {
                requireMemberId(memberId);
                yield "%s/%d/%s.%s".formatted(folder.getPath(), memberId, uuid, extension);
            }
            case JOURNAL -> {
                requireMemberId(memberId);
                if (journalId == null) {
                    log.warn("journalId 없이 JOURNAL 이미지 presigned URL 요청: memberId={}", memberId);
                    throw new CustomException(ImageErrorCode.MISSING_JOURNAL_ID);
                }
                yield "%s/%d/%d/%s.%s".formatted(folder.getPath(), memberId, journalId, uuid, extension);
            }
            case STATIC_PLACE -> {
                log.warn("presigned URL 발급 대상이 아닌 폴더 요청: folder={}", folder);
                throw new CustomException(ImageErrorCode.UNSUPPORTED_UPLOAD_FOLDER);
            }
        };
    }

    private void requireMemberId(Long memberId) {
        if (memberId == null) {
            log.warn("memberId 없이 presigned URL 요청");
            throw new CustomException(ImageErrorCode.MISSING_MEMBER_ID);
        }
    }

    // 확장자 추출
    private String getExtension(String fileName) {
        if (!fileName.contains(".") || fileName.endsWith(".")) {
            throw new CustomException(ImageErrorCode.INVALID_FILE_NAME);
        }

        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private String mapContentType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> {
                throw new CustomException(ImageErrorCode.UNSUPPORTED_FILE_EXTENSION);
            }
        };
    }

    // JOURNAL 폴더 요청일 때만 journalId 소유권을 검증한다.
    // PROFILE 등 journalId를 안 쓰는 폴더는 건드리지 않는다.
    private void validateJournalOwnership(S3Folder folder, Long memberId, Long journalId) {
        if (folder != S3Folder.JOURNAL || journalId == null ) {
            // 여행일지 신규 작성 플로우 — 사진을 먼저 올리고 journal은 나중에 생성되므로
            // 아직 존재하지 않는 journalId에 대한 소유권 검증은 건너뛴다.
            return;
        }

        boolean owned = journalRepository.existsByIdAndMember_Id(journalId, memberId);

        if (!owned) {
            throw new CustomException(ImageErrorCode.JOURNAL_ACCESS_DENIED);
        }
    }
}