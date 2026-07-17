package com.cotato.nextstation.domain.course.dto.response;

import java.time.LocalDateTime;

public record CourseCreateResponse(
        Long courseId,
        String name,
        LocalDateTime createdAt
) {
}
