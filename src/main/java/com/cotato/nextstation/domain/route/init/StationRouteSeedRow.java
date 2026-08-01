package com.cotato.nextstation.domain.route.init;

// station_route.csv 한 줄을 담는 중간 표현. 역은 아직 id가 아니라 이름 상태다.
record StationRouteSeedRow(
        String departureStationName,
        String arrivalStationName,
        int durationMinutes,
        int distanceMeters,
        Integer transferCount
) {
}
