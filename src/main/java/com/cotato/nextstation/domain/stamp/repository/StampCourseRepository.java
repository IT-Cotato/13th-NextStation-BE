package com.cotato.nextstation.domain.stamp.repository;

import com.cotato.nextstation.domain.course.entity.Course;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;

// TODO: Course 도메인의 GET /courses API 구현 완료 및 CourseQueryService으로 메서드 추가 후
//       이 Repository는 삭제하고 CourseQueryService 경유 방식으로 교체 예정
// 조회 전용이라 JpaRepository 대신 Repository(마커 인터페이스)만 상속.
public interface StampCourseRepository extends Repository<Course, Long> {

    @Query("SELECT c FROM Course c " +
            "WHERE c.stationId = :stationId " +
            "AND c.journalId IS NOT NULL " +
            "ORDER BY (c.viewCount + c.likeCount * 2) DESC")
    List<Course> findPopularCoursesByStationId(@Param("stationId") Long stationId, Pageable pageable);
}