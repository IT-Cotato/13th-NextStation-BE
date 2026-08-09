package com.cotato.nextstation.domain.place.init;

/**
 * 시딩할 장소 사진 한 장.
 * source는 사진의 출처이며, 직접 촬영본처럼 출처표시 의무가 없으면 null.
 */
record PlaceSeedImage(String imageUrl, String source) {
}
