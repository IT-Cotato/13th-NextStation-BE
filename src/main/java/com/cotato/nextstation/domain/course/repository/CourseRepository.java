package com.cotato.nextstation.domain.course.repository;

import com.cotato.nextstation.domain.course.entity.Course;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // 스크랩 가능한 코스인지 확인한다.
    // Journal을 INNER JOIN 하므로 journalId가 NULL인 코스는 자동 제외되고,
    // 삭제된 코스/일지는 각 엔티티의 @SQLRestriction으로 걸러진다.
    @Query("SELECT COUNT(c) > 0 FROM Course c " +
            "JOIN Journal j ON j.id = c.journalId " +
            "WHERE c.id = :courseId AND j.isPublic = true")
    boolean existsPublicById(@Param("courseId") Long courseId);

    // save_count는 DB에서 직접 증감시킨다.
    // 엔티티를 읽어 +1 하면 동시 스크랩 시 한쪽 증가분이 유실된다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Course c SET c.saveCount = c.saveCount + 1 WHERE c.id = :courseId")
    void increaseSaveCount(@Param("courseId") Long courseId);

    // 동시 취소로 음수가 되지 않도록 0보다 클 때만 감소시킨다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Course c SET c.saveCount = c.saveCount - 1 WHERE c.id = :courseId AND c.saveCount > 0")
    void decreaseSaveCount(@Param("courseId") Long courseId);

    /**
     * 다중/전체 취소용. 실제로 스크랩돼 있는 코스만 저장 수를 줄이고, 지운 개수를 돌려준다.
     * <p>
     * 벌크 삭제는 어떤 코스가 지워졌는지 알려주지 않으므로(MySQL에 DELETE ... RETURNING이 없다),
     * 대신 EXISTS로 "지금 스크랩이 남아 있는 코스"만 골라 감소시킨다.
     * 이미 취소된 코스가 목록에 섞여 있어도 저장 수가 과다 감소하지 않는다.
     * 반드시 삭제보다 먼저 실행해야 한다. 삭제 후에는 EXISTS가 전부 거짓이 된다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Course c SET c.saveCount = c.saveCount - 1 " +
            "WHERE c.id IN :courseIds AND c.saveCount > 0 " +
            "AND EXISTS (SELECT 1 FROM CourseSave cs " +
            "            WHERE cs.courseId = c.id AND cs.memberId = :memberId)")
    int decreaseSaveCountAll(@Param("memberId") Long memberId,
                             @Param("courseIds") Collection<Long> courseIds);

    // 역별 인기 공개 코스 조회
    // 인기순 = view_count + save_count*2, 동률이면 최신순으로 2차 정렬
    // 공개 노출 조건: journal_id가 있고 그 여행일지가 공개인 코스만
    // Course는 journalId를 Long으로만 들고 있어 Journal을 id로 ad-hoc 조인한다
    // INNER JOIN이라 journalId가 NULL인 코스는 자동 제외된다.

    @Query("SELECT c FROM Course c " +
            "JOIN Journal j ON j.id = c.journalId " +
            "WHERE c.stationId = :stationId AND j.isPublic = true " +
            "ORDER BY (c.viewCount + c.saveCount * 2) DESC, c.createdAt DESC")
    List<Course> findPopularPublicCoursesByStationId(@Param("stationId") Long stationId, Pageable pageable);

    // 내가 만든 코스 목록 (최신순). 카드에 필요한 역/대표 호선까지 한 번에 가져온다(코스마다 조회하면 N+1).
    // Course는 stationId만 들고 있어(연관관계 미매핑) Station을 id로 ad-hoc 조인한다.
    // 대표 호선이 없는 역도 있을 수 있어 LEFT JOIN으로 둔다.
    // 본인 코스이므로 공개 여부는 걸지 않는다. 삭제된 코스는 @SQLRestriction이 제외한다.
    // 호선/역 필터는 둘 다 선택 사항이라 파라미터가 null이면 조건을 건너뛴다.
    @Query("SELECT c.id AS courseId, c.name AS name, c.createdAt AS createdAt, " +
            "s.id AS stationId, s.stationName AS stationName, l.id AS lineId, l.name AS lineName " +
            "FROM Course c " +
            "JOIN Station s ON s.id = c.stationId " +
            "LEFT JOIN s.drawLine l " +
            "WHERE c.memberId = :memberId " +
            "AND (:lineId IS NULL OR l.id = :lineId) " +
            "AND (:stationId IS NULL OR s.id = :stationId) " +
            "ORDER BY c.createdAt DESC, c.id DESC")
    List<MyCourseView> findMyCourses(@Param("memberId") Long memberId,
                                     @Param("lineId") Long lineId,
                                     @Param("stationId") Long stationId,
                                     Pageable pageable);

    // 다음 페이지. 생성 시각이 같을 수 있어 id를 tie-breaker로 함께 비교한다.
    @Query("SELECT c.id AS courseId, c.name AS name, c.createdAt AS createdAt, " +
            "s.id AS stationId, s.stationName AS stationName, l.id AS lineId, l.name AS lineName " +
            "FROM Course c " +
            "JOIN Station s ON s.id = c.stationId " +
            "LEFT JOIN s.drawLine l " +
            "WHERE c.memberId = :memberId " +
            "AND (:lineId IS NULL OR l.id = :lineId) " +
            "AND (:stationId IS NULL OR s.id = :stationId) " +
            "AND (c.createdAt < :createdAt OR (c.createdAt = :createdAt AND c.id < :courseId)) " +
            "ORDER BY c.createdAt DESC, c.id DESC")
    List<MyCourseView> findMyCoursesAfterCursor(@Param("memberId") Long memberId,
                                                @Param("lineId") Long lineId,
                                                @Param("stationId") Long stationId,
                                                @Param("createdAt") LocalDateTime createdAt,
                                                @Param("courseId") Long courseId,
                                                Pageable pageable);

    // 내 코스가 하나라도 있는 호선. 코스 없는 호선 칩을 비활성화하는 데 쓴다.
    // 현재 필터와 무관하게 전체 기준으로 조회해야 필터를 바꿔 끼울 수 있다.
    // 페이징으로는 전체 목록을 볼 수 없어 서버가 따로 알려준다.
    @Query("SELECT DISTINCT l.id AS lineId, l.name AS lineName " +
            "FROM Course c " +
            "JOIN Station s ON s.id = c.stationId " +
            "JOIN s.drawLine l " +
            "WHERE c.memberId = :memberId " +
            "ORDER BY l.name")
    List<LineView> findAvailableLines(@Param("memberId") Long memberId);

    interface MyCourseView {
        Long getCourseId();
        String getName();
        LocalDateTime getCreatedAt();
        Long getStationId();
        String getStationName();
        Long getLineId();
        String getLineName();
    }

    interface LineView {
        Long getLineId();
        String getLineName();
    }
}
