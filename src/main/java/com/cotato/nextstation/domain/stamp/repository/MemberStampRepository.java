package com.cotato.nextstation.domain.stamp.repository;

import com.cotato.nextstation.domain.stamp.entity.MemberStamp;
import com.cotato.nextstation.domain.station.entity.LineCode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    // 내 스탬프 목록 조회. 카드에 필요한 역/대표 호선까지 한 번에 가져온다(스탬프마다 조회하면 N+1).
    // MemberStamp에 저장된 stationId 스냅샷으로 Station을 직접 조인한다. Course를 거치면
    // Course의 @SQLRestriction 때문에 코스가 삭제된 스탬프까지 목록에서 사라진다.
    // 같은 역에서 여러 코스를 완료해도 역/노선 정보는 항상 동일하므로 DISTINCT로 DB에서 바로
    // 역 단위로 접는다. 완료 이력이 아무리 쌓여도 응답 row 수는 방문한 distinct 역 수로 고정된다.
    // (어떤 스탬프가 최초 획득분인지는 이 목록에서 필요 없다. 필요해지면 역 단위 상세 조회를
    // 별도로 만든다.)
    @Query("SELECT DISTINCT s.id AS stationId, s.stationName AS stationName, " +
            "l.id AS lineId, l.name AS lineName, l.code AS lineCode " +
            "FROM MemberStamp ms " +
            "JOIN Station s ON s.id = ms.stationId " +
            "LEFT JOIN s.drawLine l " +
            "WHERE ms.memberId = :memberId")
    List<MyStampView> findMyStampsByMemberId(@Param("memberId") Long memberId);

    // 내 스탬프 상세 조회. 해당 역에서 회원이 완료한 여러 스탬프 중 최초(createdAt 오름차순 1건)
    // 기준으로 역/노선/획득일을 가져온다. 여행일지는 이 최초 완주 건에 한정하지 않고
    // findEarliestJournalIdByMemberIdAndStationId로 별도 조회한다(아래 메서드 주석 참고).
    @Query("SELECT s.id AS stationId, s.stationName AS stationName, " +
            "l.id AS lineId, l.name AS lineName, l.code AS lineCode, " +
            "ms.createdAt AS acquiredAt " +
            "FROM MemberStamp ms " +
            "JOIN Station s ON s.id = ms.stationId " +
            "LEFT JOIN s.drawLine l " +
            "WHERE ms.memberId = :memberId AND ms.stationId = :stationId " +
            "ORDER BY ms.createdAt ASC")
    List<MyStampDetailView> findEarliestStampByMemberIdAndStationId(@Param("memberId") Long memberId,
                                                                     @Param("stationId") Long stationId,
                                                                     Pageable pageable);

    // 해당 역에서 회원이 작성한 여행일지 중 완주 시점이 가장 이른 것의 id.
    // 최초 완주 건에는 일지가 없어도 이후 재완주 때 쓴 일지가 있으면 그걸 상세 화면에 보여주기 위해,
    // 최초 완주 스탬프가 아니라 회원이 이 역에서 완료한 스탬프 전체를 대상으로 조회한다.
    @Query("SELECT j.id FROM Journal j " +
            "JOIN MemberStamp ms ON ms.id = j.memberStampId " +
            "WHERE ms.memberId = :memberId AND ms.stationId = :stationId " +
            "ORDER BY ms.createdAt ASC")
    List<Long> findEarliestJournalIdByMemberIdAndStationId(@Param("memberId") Long memberId,
                                                            @Param("stationId") Long stationId,
                                                            Pageable pageable);

    interface MyStampView {
        Long getStationId();
        String getStationName();
        Long getLineId();
        String getLineName();
        LineCode getLineCode();
    }

    interface MyStampDetailView {
        Long getStationId();
        String getStationName();
        Long getLineId();
        String getLineName();
        LineCode getLineCode();
        LocalDateTime getAcquiredAt();
    }

}
