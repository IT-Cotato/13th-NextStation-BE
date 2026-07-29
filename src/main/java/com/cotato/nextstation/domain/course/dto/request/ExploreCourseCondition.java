package com.cotato.nextstation.domain.course.dto.request;

/**
 * 둘러보기 코스 목록의 조회 조건. 전부 선택 사항이며 null이면 그 조건을 걸지 않는다.
 * <p>
 * 노선따라 둘러보기·코스 검색·컨셉별 코스가 공개 조건과 정렬·커서를 그대로 공유하고
 * 걸러내는 기준만 달라서, 조건을 객체로 묶어 한 조회에 태운다.
 * 화면마다 조회를 따로 두면 공개 조건이 바뀔 때 한 곳을 빠뜨려 비공개 코스가 새어 나간다.
 *
 * @param lineId        호선. 역이 속한 호선 전체를 기준으로 거른다
 * @param stationId     역
 * @param keyword       검색어. 코스 이름과 역명만 대상이며 역명은 꼬리의 "역"을 떼고 비교한다
 * @param conceptTourId 컨셉투어
 */
public record ExploreCourseCondition(
        Long lineId,
        Long stationId,
        String keyword,
        Long conceptTourId
) {

    public static ExploreCourseCondition ofConceptTour(Long conceptTourId) {
        return new ExploreCourseCondition(null, null, null, conceptTourId);
    }
}
