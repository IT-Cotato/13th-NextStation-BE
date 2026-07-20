package com.cotato.nextstation.domain.recommendation.service.port;

import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.service.query.PlaceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;


 // StationPlaceReader 포트를 Place 조회 전용 서비스에 연결하는 어댑터
 // 추천 도메인이 Place DTO에 직접 의존하지 않도록 PlaceInfoResponse를 StationPlaceView로 변환한다.

@Component
@RequiredArgsConstructor
public class PlaceQueryStationPlaceReader implements StationPlaceReader {

    private final PlaceQueryService placeQueryService;

    @Override
    public List<StationPlaceView> getPlacesByStation(Long stationId) {
        return placeQueryService.getPlacesByStation(stationId).stream()
                .map(this::toView)
                .toList();
    }

    private StationPlaceView toView(PlaceInfoResponse place) {
        return new StationPlaceView(
                place.placeId(),
                place.placeName(),
                place.description(),
                place.categoryCode(),
                place.categoryName(),
                place.imageUrl(),
                place.xCoordinate(),
                place.yCoordinate()
        );
    }
}
