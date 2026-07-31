package com.cotato.nextstation.domain.member.util;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class NicknameReservedWordsFilter {

    // 부분 일치시 차단
    private static final List<String> CONTAINS_RESERVED_WORDS = List.of(
            "운영자", "관리자", "admin", "system", "official"
    );

    // 완전 일치시 차단
    private static final List<String> EXACT_RESERVED_WORDS = List.of(
            "환승여행", "nextstation"
    );

    public boolean isReservedWord(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }

        // 유저 입력값: 띄어쓰기 무시하고 소문자로 통일 (Locale.ROOT 사용하여 OS/Locale 독립적 처리)
        String normalizedNickname = nickname.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);

        // 1. 부분 일치 검사
        boolean containsBanned = CONTAINS_RESERVED_WORDS.stream()
                .anyMatch(word -> normalizedNickname.contains(word.toLowerCase(Locale.ROOT)));

        if (containsBanned) {
            return true;
        }

        // 2. 완전 일치 검사
        return EXACT_RESERVED_WORDS.stream()
                .anyMatch(word -> normalizedNickname.equals(word.toLowerCase(Locale.ROOT)));
    }
}
