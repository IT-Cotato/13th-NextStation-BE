package com.cotato.nextstation.domain.course.repository;

import com.cotato.nextstation.domain.course.entity.CoursePlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoursePlaceRepository extends JpaRepository<CoursePlace, Long> {

    List<CoursePlace> findByCourseIdOrderByOrderNumAsc(Long courseId);
}
