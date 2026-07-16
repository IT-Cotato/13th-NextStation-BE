package com.cotato.nextstation.domain.course.controller;

import com.cotato.nextstation.domain.course.dto.request.CourseCreateRequest;
import com.cotato.nextstation.domain.course.dto.request.CourseNameUpdateRequest;
import com.cotato.nextstation.domain.course.dto.request.CoursePlaceOrderUpdateRequest;
import com.cotato.nextstation.domain.course.dto.response.CourseResponse;
import com.cotato.nextstation.domain.course.service.CourseCommandService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class CourseController {

    // TODO: Auth 적용 시 X-Member-Id 헤더를 @AuthenticationPrincipal 로 교체한다.
    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    private final CourseCommandService courseCommandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<CourseResponse> createCourse(
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @Valid @RequestBody CourseCreateRequest request) {
        return CommonResponse.success(HttpStatus.CREATED, courseCommandService.createCourse(memberId, request));
    }

    @PatchMapping("/{courseId}/name")
    public CommonResponse<CourseResponse> updateCourseName(
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @PathVariable Long courseId,
            @Valid @RequestBody CourseNameUpdateRequest request) {
        return CommonResponse.success(courseCommandService.updateCourseName(memberId, courseId, request));
    }

    @PatchMapping("/{courseId}/places/order")
    public CommonResponse<CourseResponse> updateCoursePlaceOrder(
            @RequestHeader(MEMBER_ID_HEADER) Long memberId,
            @PathVariable Long courseId,
            @Valid @RequestBody CoursePlaceOrderUpdateRequest request) {
        return CommonResponse.success(courseCommandService.updateCoursePlaceOrder(memberId, courseId, request));
    }
}
