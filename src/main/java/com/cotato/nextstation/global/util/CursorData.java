package com.cotato.nextstation.global.util;

import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Base64;

// 커서 페이지네이션에서 "정렬 기준값 + tie-breaker(id)"를 인코딩/디코딩하는 범용 유틸.
// 정렬 기준이 정수(likeCount, saveCount 등)면 longValue, 시간(createdAt)이면 dateTimeValue만 채운다.
// place review 외 다른 도메인(course 등)에서도 동일한 구조로 재사용 가능.
public record CursorData(Long id, Long longValue, LocalDateTime dateTimeValue) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    public String encode() {
        try {
            byte[] json = OBJECT_MAPPER.writeValueAsBytes(this);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (JsonProcessingException e) {
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public static CursorData decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] json = Base64.getUrlDecoder().decode(cursor);
            return OBJECT_MAPPER.readValue(json, CursorData.class);
        } catch (Exception e) {
            throw new CustomException(GlobalErrorCode.INVALID_CURSOR);
        }
    }
}