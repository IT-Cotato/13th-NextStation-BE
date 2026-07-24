package com.cotato.nextstation.domain.course.repository;

import com.cotato.nextstation.domain.course.entity.CoursePlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CoursePlaceRepository extends JpaRepository<CoursePlace, Long> {

    List<CoursePlace> findByCourseIdOrderByOrderNumAsc(Long courseId);

    // 여러 코스의 장소를 한 번에 가져온다(코스마다 조회하면 N+1).
    // 카드의 장소 수·대표 태그·대표 이미지가 모두 이 결과에서 나온다.
    List<CoursePlace> findByCourseIdInOrderByCourseIdAscOrderNumAsc(Collection<Long> courseIds);

    // 이 장소를 담고 있는 코스 id
    @Query("SELECT cp.courseId FROM CoursePlace cp WHERE cp.placeId = :placeId")
    List<Long> findCourseIdsByPlaceId(@Param("placeId") Long placeId);
}
