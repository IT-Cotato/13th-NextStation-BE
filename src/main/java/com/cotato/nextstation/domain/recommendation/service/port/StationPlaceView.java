package com.cotato.nextstation.domain.recommendation.service.port;

/**
 * 역별 장소 조회 결과(seam DTO). Place(Part3) 조회 인터페이스의 반환값을 어댑터가 이 형태로 변환한다.
 * imageUrl은 장소 이미지가 없으면 카테고리 기본 이미지로 폴백된 최종 URL이다(서버 해석 완료값).
 */
public record StationPlaceView(
        Long placeId,
        String placeName,
        String description,
        String categoryCode,   // CAFE / FOOD / CULTURE / WALK
        String categoryName,
        String imageUrl,
        Double xCoordinate,
        Double yCoordinate
) {
}
