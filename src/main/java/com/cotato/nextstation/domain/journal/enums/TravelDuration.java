package com.cotato.nextstation.domain.journal.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TravelDuration {

    SHORT("3~4곳"),
    HALF_DAY("5~7곳"),
    FULL_DAY("8곳 이상");

    private final String description;
}