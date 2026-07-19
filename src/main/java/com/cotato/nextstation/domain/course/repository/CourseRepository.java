package com.cotato.nextstation.domain.course.repository;

import com.cotato.nextstation.domain.course.entity.Course;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // 역별 인기 공개 코스 조회.
    // - 인기순 = view_count + save_count*2, 동률이면 최신순(created_at DESC)으로 2차 정렬한다.
    // - 공개 노출 조건: journal_id가 있고 그 여행일지가 공개(is_public=true)인 코스만.
    //   Course는 journalId를 Long으로만 들고 있어(A안, 연관관계 없음) Journal을 id로 ad-hoc 조인한다.
    //   INNER JOIN이라 journalId가 NULL인 코스는 자동 제외된다.
    // - 삭제 코스/삭제 일지는 각 엔티티의 @SQLRestriction("is_deleted = false")으로 자동 제외되므로 조건에 명시하지 않는다.
    // ※ 이 쿼리의 필터/정렬은 Mockito 단위테스트로 검증되지 않는다(@DataJpaTest 부재).
    //    의도는 위 주석 기준이며, 변경 시 실제 DB에서 동작 확인 필요.
    @Query("SELECT c FROM Course c " +
            "JOIN Journal j ON j.id = c.journalId " +
            "WHERE c.stationId = :stationId AND j.isPublic = true " +
            "ORDER BY (c.viewCount + c.saveCount * 2) DESC, c.createdAt DESC")
    List<Course> findPopularPublicCoursesByStationId(@Param("stationId") Long stationId, Pageable pageable);
}
