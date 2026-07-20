package com.cotato.nextstation.domain.recommendation.service.port;

import java.util.List;

/**
 * 랜덤/맞춤추천이 필요로 하는 역별 장소 조회 seam.
 * 실제 구현은 Place(Part3)가 열어주는 조회 인터페이스를 어댑터로 연결한다.
 * 정인님 제공 예정 인터페이스: getPlacesByStation(stationId)
 */
public interface StationPlaceReader {

    List<StationPlaceView> getPlacesByStation(Long stationId);
}
