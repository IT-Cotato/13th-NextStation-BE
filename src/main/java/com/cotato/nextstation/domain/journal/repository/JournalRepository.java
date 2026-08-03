package com.cotato.nextstation.domain.journal.repository;

import com.cotato.nextstation.domain.journal.entity.Journal;
import com.cotato.nextstation.domain.station.entity.LineCode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface JournalRepository extends JpaRepository<Journal, Long> {

    boolean existsByIdAndMember_Id(Long journalId, Long memberId);
    boolean existsByMemberStampId(Long memberStampId);

    // 이미 여행일지가 작성된 memberStampId 목록 조회
    @Query("SELECT j.memberStampId FROM Journal j WHERE j.member.id = :memberId")
    Set<Long> findCompletedMemberStampIdsByMemberId(@Param("memberId") Long memberId);

    // 내 여행일지 목록 (최신순), 첫 페이지. 카드에 필요한 코스 좋아요 수·역·대표 호선까지 한 번에
    // 가져온다(일지마다 조회하면 N+1). Journal은 memberStampId만 들고 있어(연관관계 미매핑)
    // MemberStamp·Course·Station을 id로 ad-hoc 조인한다.
    @Query("SELECT j.id AS journalId, j.title AS title, j.createdAt AS createdAt, " +
            "c.likeCount AS likeCount, s.stationName AS stationName, " +
            "l.id AS lineId, l.name AS lineName, l.code AS lineCode " +
            "FROM Journal j " +
            "JOIN MemberStamp ms ON ms.id = j.memberStampId " +
            "JOIN Course c ON c.id = ms.courseId " +
            "JOIN Station s ON s.id = c.stationId " +
            "LEFT JOIN s.drawLine l " +
            "WHERE j.member.id = :memberId " +
            "ORDER BY j.createdAt DESC, j.id DESC")
    List<MyJournalCardView> findMyJournalCards(@Param("memberId") Long memberId, Pageable pageable);

    // 다음 페이지. 생성 시각이 같을 수 있어 id를 tie-breaker로 함께 비교한다.
    @Query("SELECT j.id AS journalId, j.title AS title, j.createdAt AS createdAt, " +
            "c.likeCount AS likeCount, s.stationName AS stationName, " +
            "l.id AS lineId, l.name AS lineName, l.code AS lineCode " +
            "FROM Journal j " +
            "JOIN MemberStamp ms ON ms.id = j.memberStampId " +
            "JOIN Course c ON c.id = ms.courseId " +
            "JOIN Station s ON s.id = c.stationId " +
            "LEFT JOIN s.drawLine l " +
            "WHERE j.member.id = :memberId " +
            "AND (j.createdAt < :createdAt OR (j.createdAt = :createdAt AND j.id < :journalId)) " +
            "ORDER BY j.createdAt DESC, j.id DESC")
    List<MyJournalCardView> findMyJournalCardsAfterCursor(@Param("memberId") Long memberId,
                                                          @Param("createdAt") LocalDateTime createdAt,
                                                          @Param("journalId") Long journalId,
                                                          Pageable pageable);

    interface MyJournalCardView {
        Long getJournalId();
        String getTitle();
        LocalDateTime getCreatedAt();
        int getLikeCount();
        String getStationName();
        Long getLineId();
        String getLineName();
        LineCode getLineCode();
    }
}