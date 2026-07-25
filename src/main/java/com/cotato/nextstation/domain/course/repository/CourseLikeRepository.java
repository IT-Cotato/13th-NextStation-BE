package com.cotato.nextstation.domain.course.repository;

import com.cotato.nextstation.domain.course.entity.CourseLike;
import com.cotato.nextstation.domain.station.entity.LineCode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface CourseLikeRepository extends JpaRepository<CourseLike, Long> {

    boolean existsByMemberIdAndCourseId(Long memberId, Long courseId);

    // 여러 코스의 좋아요 여부를 한 번에 조회
    @Query("SELECT cs.courseId FROM CourseLike cs " +
            "WHERE cs.memberId = :memberId AND cs.courseId IN :courseIds")
    List<Long> findLikedCourseIds(@Param("memberId") Long memberId,
                                  @Param("courseIds") Collection<Long> courseIds);

    // 좋아요을 삭제하고 실제로 지워진 행 수를 돌려준다.
    // 조회 후 삭제하면 동시 취소 시 두 요청이 모두 "있다"고 보고 좋아요 수를 각각 줄이게 되므로,
    // 삭제 쿼리의 영향 행 수를 확인 수단으로 삼아 실제로 지운 요청만 좋아요 수를 줄인다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CourseLike cs " +
            "WHERE cs.memberId = :memberId AND cs.courseId = :courseId")
    int deleteByMemberIdAndCourseId(@Param("memberId") Long memberId,
                                    @Param("courseId") Long courseId);

    // 여러 좋아요을 한 번에 삭제한다. 코스마다 삭제 쿼리를 날리면 좋아요 수에 비례해 비용이 늘어난다.
    // 어떤 코스가 지워졌는지는 알 수 없으므로, 좋아요 수 감소를 이 쿼리보다 먼저 실행해야 한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CourseLike cs " +
            "WHERE cs.memberId = :memberId AND cs.courseId IN :courseIds")
    int deleteByMemberIdAndCourseIdIn(@Param("memberId") Long memberId,
                                      @Param("courseIds") Collection<Long> courseIds);

    // "모두 선택" 대상이 되는 코스 id. 목록에 실제로 보이는 것과 같은 조건을 걸어야
    // 화면에 뜨지도 않은 좋아요이 함께 취소되는 일이 없다.
    @Query("SELECT c.id FROM CourseLike cs " +
            "JOIN Course c ON c.id = cs.courseId " +
            "JOIN Journal j ON j.id = c.journalId " +
            "WHERE cs.memberId = :memberId AND j.isPublic = true")
    List<Long> findVisibleLikedCourseIds(@Param("memberId") Long memberId);

    // 좋아요한 코스 목록 (최근 좋아요순). 카드에 필요한 역/대표 호선까지 한 번에 가져온다(코스마다 조회하면 N+1).
    // 좋아요은 원본 참조라 원본이 삭제되거나 비공개로 바뀌면 목록에서도 빠져야 한다.
    // Journal INNER JOIN으로 비공개·일지 없는 코스가 걸러지고, 삭제된 코스/일지는 @SQLRestriction이 처리한다.
    // Course는 stationId만 들고 있어(연관관계 미매핑) Station을 id로 ad-hoc 조인한다.
    // 대표 호선이 없는 역도 있을 수 있어 LEFT JOIN으로 둔다.
    @Query("SELECT cs.id AS likeId, cs.createdAt AS likedAt, c.id AS courseId, c.name AS name, " +
            "s.id AS stationId, s.stationName AS stationName, " +
            "l.id AS lineId, l.name AS lineName, l.code AS lineCode " +
            "FROM CourseLike cs " +
            "JOIN Course c ON c.id = cs.courseId " +
            "JOIN Journal j ON j.id = c.journalId " +
            "JOIN Station s ON s.id = c.stationId " +
            "LEFT JOIN s.drawLine l " +
            "WHERE cs.memberId = :memberId AND j.isPublic = true " +
            "ORDER BY cs.createdAt DESC, cs.id DESC")
    List<LikedCourseView> findLikedCourses(@Param("memberId") Long memberId, Pageable pageable);

    // 다음 페이지. 정렬 기준이 좋아요 시각이라 커서도 코스 id가 아닌 course_like.id를 tie-breaker로 쓴다.
    @Query("SELECT cs.id AS likeId, cs.createdAt AS likedAt, c.id AS courseId, c.name AS name, " +
            "s.id AS stationId, s.stationName AS stationName, " +
            "l.id AS lineId, l.name AS lineName, l.code AS lineCode " +
            "FROM CourseLike cs " +
            "JOIN Course c ON c.id = cs.courseId " +
            "JOIN Journal j ON j.id = c.journalId " +
            "JOIN Station s ON s.id = c.stationId " +
            "LEFT JOIN s.drawLine l " +
            "WHERE cs.memberId = :memberId AND j.isPublic = true " +
            "AND (cs.createdAt < :likedAt OR (cs.createdAt = :likedAt AND cs.id < :likeId)) " +
            "ORDER BY cs.createdAt DESC, cs.id DESC")
    List<LikedCourseView> findLikedCoursesAfterCursor(@Param("memberId") Long memberId,
                                                      @Param("likedAt") LocalDateTime likedAt,
                                                      @Param("likeId") Long likeId,
                                                      Pageable pageable);

    interface LikedCourseView {
        Long getLikeId();
        LocalDateTime getLikedAt();
        Long getCourseId();
        String getName();
        Long getStationId();
        String getStationName();
        Long getLineId();
        String getLineName();
        LineCode getLineCode();
    }
}
