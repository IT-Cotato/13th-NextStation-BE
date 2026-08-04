package com.cotato.nextstation.domain.stamp.repository;

import com.cotato.nextstation.domain.stamp.entity.MemberStamp;
import com.cotato.nextstation.domain.station.entity.LineCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface MemberStampRepository extends JpaRepository<MemberStamp, Long> {

    boolean existsByMemberIdAndId(Long memberId, Long id);
    boolean existsByMemberIdAndCourseId(Long memberId, Long courseId);


    // 넘긴 코스들 중 해당 회원이 완료한 코스 id만 반환한다.
    // 코스 목록은 한 페이지에 여러 건이라 코스마다 단건 조회하면 쿼리가 그만큼 나간다.
    @Query("SELECT ms.courseId FROM MemberStamp ms " +
            "WHERE ms.memberId = :memberId AND ms.courseId IN :courseIds")
    List<Long> findCompletedCourseIds(@Param("memberId") Long memberId,
                                      @Param("courseIds") List<Long> courseIds);

    @Query("SELECT ms FROM MemberStamp ms " +
            "WHERE ms.memberId = :memberId " +
            "AND NOT EXISTS (" +
            "    SELECT j FROM Journal j " +
            "    WHERE j.memberStampId = ms.id" +
            ") " +
            "ORDER BY ms.createdAt DESC")
    List<MemberStamp> findUncompletedByMemberId(@Param("memberId") Long memberId);

    List<MemberStamp> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    // 완료된 스탬프 제외하고 미작성 스탬프만 조회 (최신순)
    List<MemberStamp> findByMemberIdAndIdNotInOrderByCreatedAtDesc(
            Long memberId, Set<Long> completedStampIds);

    // 다른 회원 프로필 - 방문한(스탬프를 찍은) 서로 다른 역의 개수.
    // MemberStamp는 courseId만 들고 있어(연관관계 미매핑) Course를 id로 ad-hoc 조인한다.
    @Query("SELECT COUNT(DISTINCT c.stationId) FROM MemberStamp ms " +
            "JOIN Course c ON c.id = ms.courseId " +
            "WHERE ms.memberId = :memberId")
    long countVisitedStations(@Param("memberId") Long memberId);

    // 다른 회원의 스탬프 탭 - 방문한 역을 역/대표 호선과 함께 중복 없이 조회한다(역 하나당 스탬프 1개).
    // 내 스탬프 목록(MyStampListResponse)과 같은 방식으로 대표 호선(station.draw_line)만 내려주고,
    // 1~9호선 정렬 + 동일 호선 내 역명 가나다순 정렬은 서비스 레이어에서 처리한다.
    // MemberStamp는 courseId만 들고 있어(연관관계 미매핑) Course를 id로 ad-hoc 조인한다.
    @Query("SELECT DISTINCT s.id AS stationId, s.stationName AS stationName, " +
            "l.id AS lineId, l.name AS lineName, l.code AS lineCode " +
            "FROM MemberStamp ms " +
            "JOIN Course c ON c.id = ms.courseId " +
            "JOIN Station s ON s.id = c.stationId " +
            "LEFT JOIN s.drawLine l " +
            "WHERE ms.memberId = :memberId")
    List<MemberStampView> findMemberStampsByMemberId(@Param("memberId") Long memberId);

    interface MemberStampView {
        Long getStationId();
        String getStationName();
        Long getLineId();
        String getLineName();
        LineCode getLineCode();
    }

}
