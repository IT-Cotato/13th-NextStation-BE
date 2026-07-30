package com.cotato.nextstation.domain.member.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class NicknameReservedWordsFilterTest {

    private final NicknameReservedWordsFilter filter = new NicknameReservedWordsFilter();

    @ParameterizedTest
    @ValueSource(strings = {
            "운영자", "최고운영자", "운영자1", "관리자", "시스템관리자",
            "admin", "ADMIN", "Administrator", "System", "official"
    })
    @DisplayName("사칭 키워드가 부분 포함된 닉네임은 예약어로 판별된다")
    void isReservedWord_containsBannedWords(String nickname) {
        assertThat(filter.isReservedWord(nickname)).isTrue();
    }

    @Test
    @DisplayName("브랜드명 명칭과 완전 일치하는 닉네임은 예약어로 판별된다")
    void isReservedWord_exactBrandNameMatch() {
        assertThat(filter.isReservedWord("환승여행")).isTrue();
        assertThat(filter.isReservedWord("NEXTSTATION")).isTrue();
        assertThat(filter.isReservedWord("환 승 여 행")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "환승여행자", "환승여행러", "환승여행기", "일반유저", "길동이123"
    })
    @DisplayName("정상 닉네임 및 브랜드명이 부분 포함된 복합어 닉네임은 허용된다")
    void isReservedWord_allowedNicknames(String nickname) {
        assertThat(filter.isReservedWord(nickname)).isFalse();
    }

    @Test
    @DisplayName("터키어(tr-TR) Locale 환경에서도 대문자 영문 예약어가 올바르게 차단된다")
    void isReservedWord_turkishLocale() {
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(filter.isReservedWord("ADMIN")).isTrue();
            assertThat(filter.isReservedWord("NEXTSTATION")).isTrue();
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
