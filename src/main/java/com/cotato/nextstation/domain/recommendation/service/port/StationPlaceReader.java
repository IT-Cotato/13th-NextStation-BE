package com.cotato.nextstation.domain.recommendation.service.port;

import java.util.List;


 // 랜덤/맞춤추천이 필요로 하는 역별 장소 조회 seam
public interface StationPlaceReader {

    List<StationPlaceView> getPlacesByStation(Long stationId);
}
