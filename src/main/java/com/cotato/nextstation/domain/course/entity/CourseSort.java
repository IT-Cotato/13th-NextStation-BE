package com.cotato.nextstation.domain.course.entity;

/**
 * 둘러보기 목록의 정렬 기준. 화면의 "인기순 / 최신순" 토글에 대응한다.
 * <p>
 * 이 값을 받는 둘러보기 코스 목록·컨셉별 코스 목록 두 API 모두 정렬이 선택 사항이고,
 * 생략하면 {@link #POPULAR}로 조회한다. 정렬 토글이 없는 화면(코스 검색, 둘러보기 메인의
 * 노선 섹션)이 이 기본값을 그대로 쓴다.
 * <p>
 * 기본값을 두는 것은 정렬 기준이 없으면 페이지마다 순서가 흔들려 커서 페이징이
 * 성립하지 않기 때문이다.
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
