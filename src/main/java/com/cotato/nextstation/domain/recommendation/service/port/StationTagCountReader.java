package com.cotato.nextstation.domain.recommendation.service.port;

import java.util.List;
import java.util.Map;

// 맞춤추천 점수 계산에 쓰는 역별/태그별 장소 수 집계 seam
public interface StationTagCountReader {

    // 반환: stationId -> (태그명 -> 장소 수). 장소가 없는 역/태그 조합은 키 자체가 없다.
    Map<Long, Map<String, Long>> getPlaceCountsByStationForTags(List<String> tags);
}
