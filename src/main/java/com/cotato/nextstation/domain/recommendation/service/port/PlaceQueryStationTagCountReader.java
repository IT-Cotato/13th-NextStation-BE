package com.cotato.nextstation.domain.recommendation.service.port;

import com.cotato.nextstation.domain.place.service.query.PlaceInfoQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// StationTagCountReader 포트를 Place 조회 전용 서비스에 연결하는 어댑터
@Component
@RequiredArgsConstructor
public class PlaceQueryStationTagCountReader implements StationTagCountReader {

    private final PlaceInfoQueryService placeInfoQueryService;

    @Override
    public Map<Long, Map<String, Long>> getPlaceCountsByStationForTags(List<String> tags) {
        return placeInfoQueryService.getPlaceCountsByStationForTags(tags).counts();
    }
}
