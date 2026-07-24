package com.cotato.nextstation.domain.course.repository;

import com.cotato.nextstation.domain.course.entity.Course;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 다중 취소용. 코스마다 UPDATE를 날리지 않고 한 번에 처리한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Course c SET c.saveCount = c.saveCount - 1 " +
            "WHERE c.id IN :courseIds AND c.saveCount > 0")
    void decreaseSaveCountAll(@Param("courseIds") Collection<Long> courseIds);

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

    // 특정 장소를 담고 있는 공개 코스를 인기순으로 조회한다 (장소 상세 화면 하단).
    // 카드에 필요한 역·대표 호선을 함께 가져온다(코스마다 조회하면 N+1).
    // 노출 조건과 인기순 공식은 위 역별 인기 코스와 같다.
    @Query("SELECT c.id AS courseId, c.name AS name, " +
            "s.id AS stationId, s.stationName AS stationName, l.id AS lineId, l.name AS lineName " +
            "FROM CoursePlace cp " +
            "JOIN Course c ON c.id = cp.courseId " +
            "JOIN Journal j ON j.id = c.journalId " +
            "JOIN Station s ON s.id = c.stationId " +
            "LEFT JOIN s.drawLine l " +
            "WHERE cp.placeId = :placeId AND j.isPublic = true " +
            "ORDER BY (c.viewCount + c.saveCount * 2) DESC, c.createdAt DESC, c.id DESC")
    List<PlaceCourseView> findPopularPublicCoursesByPlaceId(@Param("placeId") Long placeId, Pageable pageable);

    interface PlaceCourseView {
        Long getCourseId();
        String getName();
        Long getStationId();
        String getStationName();
        Long getLineId();
        String getLineName();
    }
}
