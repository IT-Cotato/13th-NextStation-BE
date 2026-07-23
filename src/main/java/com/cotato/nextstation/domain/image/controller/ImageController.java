package com.cotato.nextstation.domain.image.controller;

import com.cotato.nextstation.domain.image.dto.request.PresignedUrlRequest;
import com.cotato.nextstation.domain.image.dto.response.PresignedUrlResponse;
import com.cotato.nextstation.domain.image.service.command.ImageCommandService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
public class ImageController {

    private final ImageCommandService imageCommandService;

    @Operation(
            summary = "단일 이미지 업로드용 presigned URL 발급",
            description = """
                    S3에 이미지를 직접 업로드할 수 있는 presigned URL을 발급한다.
                    - 발급받은 presignedUrl로 이미지 바이너리를 PUT 요청하면 업로드가 완료된다.
                        - Content-Type 헤더에 응답의 contentType을 그대로 실어야 한다.
                    - presignedUrl은 10분 후 만료된다.
                    - 업로드 완료 후, 응답의 imageUrl을 프로필 설정 API 등 이미지 URL이 필요한 다음 요청에 그대로 실어 보내면 된다.
                    - folder는 도메인에 맞추어서 요청한다. (PROFILE: 프로필 이미지, JOURNAL: 여행일지 이미지)
                       - 아래 Request body의 Schema 설명 참고
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발급 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패, 확장자 없는 파일명, 또는 지원하지 않는 확장자 (`GlobalErrorCode.VALIDATION_ERROR`, `ImageErrorCode.INVALID_FILE_NAME`, `ImageErrorCode.UNSUPPORTED_FILE_EXTENSION`)"),
    })
    @PostMapping("/presigned-url")
    public CommonResponse<PresignedUrlResponse> getPresignedUrl(@Valid @RequestBody PresignedUrlRequest request) {
        return CommonResponse.success(imageCommandService.getPresignedUrl(
                request.folder(), request.memberId(), request.journalId(), request.fileName()));
    }
}