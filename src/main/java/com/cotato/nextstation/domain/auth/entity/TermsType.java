package com.cotato.nextstation.domain.auth.entity;

import lombok.Getter;

import java.util.Arrays;

/**
 * 약관 종류.
 *
 * <p>{@code title}은 동의 화면에 노출되는 항목 이름이고 {@code documentTitle}은 원문을 여는 화면의 제목이다.
 * 개인정보처리방침처럼 동의받는 항목의 이름과 문서 자체의 이름이 다른 경우가 있어 구분한다.
 * {@code title}은 {@code terms_consents} 조회 키로도 쓰이므로 DB 값과 반드시 일치해야 한다.
 */
@Getter
public enum TermsType {

    SERVICE("서비스 이용약관", "서비스 이용약관"),
    PRIVACY("개인정보 수집 및 이용 동의", "개인정보처리방침"),
    MARKETING("마케팅 정보 수신 동의", "마케팅 정보 수신 동의");

    private final String title;
    private final String documentTitle;

    TermsType(String title, String documentTitle) {
        this.title = title;
        this.documentTitle = documentTitle;
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