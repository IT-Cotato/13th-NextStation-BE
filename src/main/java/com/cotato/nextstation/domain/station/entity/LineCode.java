package com.cotato.nextstation.domain.station.entity;

import java.util.Arrays;

public enum LineCode {

    LINE_1("1호선"),
    LINE_2("2호선"),
    LINE_3("3호선"),
    LINE_4("4호선"),
    LINE_5("5호선"),
    LINE_6("6호선"),
    LINE_7("7호선"),
    LINE_8("8호선"),
    LINE_9("9호선"),
    GYEONGUI_JUNGANG("경의중앙선"),
    GYEONGCHUN("경춘선"),
    GONGHANG("공항철도"),
    GIMPO_GOLD("김포골드라인"),
    SEOHAE("서해선"),
    SUIN_BUNDANG("수인분당선"),
    SILLIM("신림선"),
    SINBUNDANG("신분당선"),
    UI_SINSEOL("우이신설선");

    private final String displayName;

    LineCode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static LineCode from(String displayName) {
        return Arrays.stream(values())
                .filter(code -> code.displayName.equals(displayName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("정의되지 않은 노선명입니다: " + displayName));
    }
}
