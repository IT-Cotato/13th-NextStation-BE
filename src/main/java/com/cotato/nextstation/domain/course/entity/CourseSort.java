package com.cotato.nextstation.domain.course.entity;

/**
 * 둘러보기 목록의 정렬 기준. 화면의 "전체 / 최신순 / 인기순" 토글에 대응한다.
 * <p>
 * 화면의 "전체"는 정렬 없음이 아니라 기본값을 뜻하므로 {@link #LATEST}로 받는다.
 * 정렬 기준이 없으면 페이지마다 순서가 흔들려 커서 페이징이 성립하지 않는다.
 */
public enum CourseSort {

    /** 최신순. course.created_at 내림차순 */
    LATEST,

    /**
     * 인기순. view_count + like_count × 2 내림차순, 동률이면 최신순.
     * <p>
     * "사람들이 많이 찾는 코스"(좋아요 수 기준)와는 다른 값이다. 그쪽은 별도 API를 쓴다.
     */
    POPULAR
}
