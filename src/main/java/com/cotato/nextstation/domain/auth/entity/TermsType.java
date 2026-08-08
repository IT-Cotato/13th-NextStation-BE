package com.cotato.nextstation.domain.auth.entity;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum TermsType {

    SERVICE("서비스 이용약관"),
    PRIVACY("개인정보 수집 및 이용 동의"),
    MARKETING("마케팅 정보 수신 동의");

    private final String title;

    TermsType(String title) {
        this.title = title;
    }

    /**
     * 매핑되는 종류가 없으면 null. 시더가 만들지 않은 약관 row(수동 등록 등)도 목록에는 나와야 하므로 예외로 막지 않는다.
     */
    public static TermsType fromTitle(String title) {
        return Arrays.stream(values())
                .filter(type -> type.title.equals(title))
                .findFirst()
                .orElse(null);
    }
}