package com.cotato.nextstation.domain.recommendation.service.port;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 임시 stub. Place(Part3)의 역별 장소 조회 인터페이스(getPlacesByStation)가 도착하면
 * 이 클래스를 제거하고 실제 어댑터로 교체한다.
 * 그때까지 랜덤뽑기는 역 정보/로그는 정상 동작하고 코스 미리보기만 빈 목록으로 내려간다.
 * TODO: 정인님 Place 조회 인터페이스 연결 후 삭제
 */
@Slf4j
@Component
public class StubStationPlaceReader implements StationPlaceReader {

    @PostConstruct
    void warnStubInUse() {
        log.warn("StationPlaceReader stub 사용 중 — Place 조회 인터페이스 연결 전까지 코스 미리보기는 빈 목록으로 응답됩니다.");
    }

    @Override
    public List<StationPlaceView> getPlacesByStation(Long stationId) {
        return List.of();
    }
}
