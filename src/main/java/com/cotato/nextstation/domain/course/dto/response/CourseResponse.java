package com.cotato.nextstation.domain.course.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CourseResponse(
        Long id,
        String name,
        Long stationId,
        Long conceptTourId,
        Long journalId,
        int viewCount,
        int saveCount,
        LocalDateTime createdAt,
        List<CoursePlaceResponse> places
) {
}
