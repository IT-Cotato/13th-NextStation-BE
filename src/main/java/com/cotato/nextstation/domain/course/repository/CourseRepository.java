package com.cotato.nextstation.domain.course.repository;

import com.cotato.nextstation.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByIdAndIsDeletedFalse(Long id);
}
