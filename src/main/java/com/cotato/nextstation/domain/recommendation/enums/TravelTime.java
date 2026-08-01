package com.cotato.nextstation.domain.recommendation.enums;

// 맞춤추천 이동 가능 시간. 출발역에서 뽑기 대상 역까지의 소요시간 상한으로 쓰인다.
public enum TravelTime {

    THIRTY_MINUTES(30),
    ONE_HOUR(60),
    // 상관없음: 시간 제한 없이 도달 가능한 전 구간을 후보로 둔다.
    ANY(null);

    private final Integer maxDurationMinutes;

    TravelTime(Integer maxDurationMinutes) {
        this.maxDurationMinutes = maxDurationMinutes;
    }

    public boolean hasLimit() {
        return maxDurationMinutes != null;
    }

    public int getMaxDurationMinutes() {
        return maxDurationMinutes;
    }
}
