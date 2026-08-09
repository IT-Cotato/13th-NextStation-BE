package com.cotato.nextstation.domain.place.init;

/**
 * 시딩할 장소 사진 한 장.
 * source는 사진의 출처. 수집 경로를 거치지 않은 사진은 업로드 배치가 "직접 촬영"으로 채운다.
 */
record PlaceSeedImage(String imageUrl, String source) {
}
