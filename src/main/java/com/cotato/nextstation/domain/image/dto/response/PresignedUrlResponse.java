package com.cotato.nextstation.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// 단일 이미지 업로드 전용 응답, 다중 업로드는 별도 API(List<PresignedUrlResponse>)로 구현 필요
@Schema(description = "이미지 업로드용 presigned URL 발급 응답")
public record PresignedUrlResponse(

        @Schema(description = "S3에 이미지를 직접 PUT할 때 쓰는 presigned URL. 10분 후 만료된다.", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/uploads/profile/uuid_profile.jpg?X-Amz-Signature=...")
        String presignedUrl,

        @Schema(description = "업로드 완료 후 실제로 접근 가능한 이미지 URL. 다른 API(프로필 설정 등) 요청에 이 값을 그대로 실어 보내면 된다.", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/uploads/profile/uuid_profile.jpg")
        String imageUrl,

        @Schema(description = "PUT 요청 시 Content-Type 헤더에 그대로 실어야 하는 값", example = "image/jpeg")
        String contentType
) {
}