package com.cotato.nextstation.domain.course.repository;

import com.cotato.nextstation.domain.course.entity.Course;
import com.cotato.nextstation.domain.station.entity.LineCode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // 다중 삭제 대상 조회. memberId로 걸러서 남의 코스는 애초에 대상에서 빠진다(부분 성공 허용).
    List<Course> findAllByMemberIdAndIdIn(Long memberId, List<Long> ids);

    // 코스 확인 화면. 헤더의 역 이름까지 한 번에 가져온다.
    // memberId를 조건에 넣어 남의 코스는 애초에 조회되지 않는다(존재 여부도 알리지 않는다).
    // 공개 조건은 걸지 않는다. 본인 코스는 일지를 안 썼거나 비공개여도 보여야 한다.
    // Course는 stationId만 들고 있어(연관관계 미매핑) Station을 id로 ad-hoc 조인한다.
    @Query("SELECT c.id AS courseId, c.name AS name, " +
            "s.id AS stationId, s.stationName AS stationName " +
            "FROM Course c " +
            "JOIN Station s ON s.id = c.stationId " +
            "WHERE c.id = :courseId AND c.memberId = :memberId")
    Optional<MyCourseDetailView> findMyCourseDetail(@Param("memberId") Long memberId,
                                                    @Param("courseId") Long courseId);

    // 좋아요 가능한 코스인지 확인한다.
    // Journal을 INNER JOIN 하므로 journalId가 NULL인 코스는 자동 제외되고,
    // 삭제된 코스/일지는 각 엔티티의 @SQLRestriction으로 걸러진다.
    @Query("SELECT COUNT(c) > 0 FROM Course c " +
            "JOIN Journal j ON j.id = c.journalId " +
            "WHERE c.id = :courseId AND j.isPublic = true")
    boolean existsPublicById(@Param("courseId") Long courseId);

    /**
     * 코스 상세를 열 때 조회수를 올린다.
     * <p>
     * like_count와 같은 이유로 DB에서 직접 증가시킨다. 엔티티를 읽어 +1 하면
     * 동시 조회 시 한쪽 증가분이 유실된다.
     * <p>
     * 본인이 자기 코스를 열었을 때는 올리지 않는다. 저장 탭에서 자기 코스를 드나들 때마다
     * 오르면 인기순(view_count + like_count×2)이 자주 열어본 사람 순으로 부풀려진다.
     * 비로그인 조회는 본인일 수 없으므로 항상 올린다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Course c SET c.viewCount = c.viewCount + 1 " +
            "WHERE c.id = :courseId AND (:viewerMemberId IS NULL OR c.memberId <> :viewerMemberId)")
    int increaseViewCount(@Param("courseId") Long courseId,
                          @Param("viewerMemberId") Long viewerMemberId);

    // like_count는 DB에서 직접 증감시킨다.
    // 엔티티를 읽어 +1 하면 동시 좋아요 시 한쪽 증가분이 유실된다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Course c SET c.likeCount = c.likeCount + 1 WHERE c.id = :courseId")
    void increaseLikeCount(@Param("courseId") Long courseId);

    // 동시 취소로 음수가 되지 않도록 0보다 클 때만 감소시킨다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Course c SET c.likeCount = c.likeCount - 1 WHERE c.id = :courseId AND c.likeCount > 0")
    void decreaseLikeCount(@Param("courseId") Long courseId);

    /**
     * 다중/전체 취소용. 실제로 좋아요돼 있는 코스만 좋아요 수를 줄이고, 지운 개수를 돌려준다.
     * <p>
     * 벌크 삭제는 어떤 코스가 지워졌는지 알려주지 않으므로(MySQL에 DELETE ... RETURNING이 없다),
     * 대신 EXISTS로 "지금 좋아요가 남아 있는 코스"만 골라 감소시킨다.
     * 이미 취소된 코스가 목록에 섞여 있어도 좋아요 수가 과다 감소하지 않는다.
     * 반드시 삭제보다 먼저 실행해야 한다. 삭제 후에는 EXISTS가 전부 거짓이 된다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Course c SET c.likeCount = c.likeCount - 1 " +
            "WHERE c.id IN :courseIds AND c.likeCount > 0 " +
            "AND EXISTS (SELECT 1 FROM CourseLike cs " +
            "            WHERE cs.courseId = c.id AND cs.memberId = :memberId)")
    int decreaseLikeCountAll(@Param("memberId") Long memberId,
                             @Param("courseIds") Collection<Long> courseIds);

    // 역별 인기 공개 코스 조회
    // 인기순 = view_count + like_count*2, 동률이면 최신순으로 2차 정렬
    // 공개 노출 조건: journal_id가 있고 그 여행일지가 공개인 코스만
    // Course는 journalId를 Long으로만 들고 있어 Journal을 id로 ad-hoc 조인한다
    // INNER JOIN이라 journalId가 NULL인 코스는 자동 제외된다.

    @Query("SELECT c FROM Course c " +
            "JOIN Journal j ON j.id = c.journalId " +
            "WHERE c.stationId = :stationId AND j.isPublic = true " +
            "ORDER BY (c.viewCount + c.likeCount * 2) DESC, c.createdAt DESC")
    List<Course> findPopularPublicCoursesByStationId(@Param("stationId") Long stationId, Pageable pageable);

    // 내가 만든 코스 목록 (최신순). 카드에 필요한 역/대표 호선까지 한 번에 가져온다(코스마다 조회하면 N+1).
    // Course는 stationId만 들고 있어(연관관계 미매핑) Station을 id로 ad-hoc 조인한다.
    // 대표 호선이 없는 역도 있을 수 있어 LEFT JOIN으로 둔다.
    // 본인 코스이므로 공개 여부는 걸지 않는다. 삭제된 코스는 @SQLRestriction이 제외한다.
    // 호선/역 필터는 둘 다 선택 사항이라 파라미터가 null이면 조건을 건너뛴다.
    //
    // 호선 필터는 대표 호선이 아니라 역이 속한 호선 전체(StationLine)를 기준으로 판단한다.
    // 대표 호선은 카드 배지에 뭘 보여줄지 정한 표시용 값이라, 그걸로 걸러내면 환승역 코스가
    // 실제로 갈 수 있는 다른 호선 탭에서 사라진다(예: 동묘앞역 코스가 6호선 탭에서 누락).
    @Query("SELECT c.id AS courseId, c.name AS name, c.createdAt AS createdAt, " +
            "s.id AS stationId, s.stationName AS stationName, " +
            "l.id AS lineId, l.name AS lineName, l.code AS lineCode " +
            "FROM Course c " +
            "JOIN Station s ON s.id = c.stationId " +
            "LEFT JOIN s.drawLine l " +
            "WHERE c.memberId = :memberId " +
            "AND (:lineId IS NULL OR EXISTS (SELECT 1 FROM StationLine sl " +
            "     WHERE sl.station.id = s.id AND sl.line.id = :lineId)) " +
            "AND (:stationId IS NULL OR s.id = :stationId) " +
            "ORDER BY c.createdAt DESC, c.id DESC")
    List<MyCourseView> findMyCourses(@Param("memberId") Long memberId,
                                     @Param("lineId") Long lineId,
                                     @Param("stationId") Long stationId,
                                     Pageable pageable);

    // 다음 페이지. 생성 시각이 같을 수 있어 id를 tie-breaker로 함께 비교한다.
    @Query("SELECT c.id AS courseId, c.name AS name, c.createdAt AS createdAt, " +
            "s.id AS stationId, s.stationName AS stationName, " +
            "l.id AS lineId, l.name AS lineName, l.code AS lineCode " +
            "FROM Course c " +
            "JOIN Station s ON s.id = c.stationId " +
            "LEFT JOIN s.drawLine l " +
            "WHERE c.memberId = :memberId " +
            "AND (:lineId IS NULL OR EXISTS (SELECT 1 FROM StationLine sl " +
            "     WHERE sl.station.id = s.id AND sl.line.id = :lineId)) " +
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
    // 호선 필터와 같은 기준(역이 속한 호선 전체)으로 조회해야 한다. 기준이 다르면
    // "칩은 비활성인데 필터를 걸면 결과가 나오는" 불일치가 생긴다.
    @Query("SELECT DISTINCT l.id AS lineId, l.name AS lineName, l.code AS lineCode " +
            "FROM Course c " +
            "JOIN Station s ON s.id = c.stationId " +
            "JOIN StationLine sl ON sl.station.id = s.id " +
            "JOIN sl.line l " +
            "WHERE c.memberId = :memberId " +
            "ORDER BY l.name")
    List<LineView> findAvailableLines(@Param("memberId") Long memberId);

    // 특정 장소를 담고 있는 공개 코스를 인기순으로 조회한다 (장소 상세 화면 하단).
    // 카드에 필요한 역·대표 호선을 함께 가져온다(코스마다 조회하면 N+1).
    // 노출 조건과 인기순 공식은 위 역별 인기 코스와 같다.
    @Query("SELECT c.id AS courseId, c.name AS name, " +
            "s.id AS stationId, s.stationName AS stationName, " +
            "l.id AS lineId, l.name AS lineName, l.code AS lineCode " +
            "FROM CoursePlace cp " +
            "JOIN Course c ON c.id = cp.courseId " +
            "JOIN Journal j ON j.id = c.journalId " +
            "JOIN Station s ON s.id = c.stationId " +
            "LEFT JOIN s.drawLine l " +
            "WHERE cp.placeId = :placeId AND j.isPublic = true " +
            "ORDER BY (c.viewCount + c.likeCount * 2) DESC, c.createdAt DESC, c.id DESC")
    List<PlaceCourseView> findPopularPublicCoursesByPlaceId(@Param("placeId") Long placeId, Pageable pageable);

    interface MyCourseDetailView {
        Long getCourseId();
        String getName();
        Long getStationId();
        String getStationName();
    }

    interface MyCourseView {
        Long getCourseId();
        String getName();
        LocalDateTime getCreatedAt();
        Long getStationId();
        String getStationName();
        Long getLineId();
        String getLineName();
        LineCode getLineCode();
    }

    interface PlaceCourseView {
        Long getCourseId();
        String getName();
        Long getStationId();
        String getStationName();
        Long getLineId();
        String getLineName();
        LineCode getLineCode();
    }

    interface LineView {
        Long getLineId();
        String getLineName();
        LineCode getLineCode();
    }
}
