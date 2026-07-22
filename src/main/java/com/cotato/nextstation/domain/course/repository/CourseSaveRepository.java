package com.cotato.nextstation.domain.course.repository;

import com.cotato.nextstation.domain.course.entity.CourseSave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseSaveRepository extends JpaRepository<CourseSave, Long> {

    boolean existsByMemberIdAndCourseId(Long memberId, Long courseId);

    Optional<CourseSave> findByMemberIdAndCourseId(Long memberId, Long courseId);

    // 여러 코스의 스크랩 여부를 한 번에 조회 
    @Query("SELECT cs.courseId FROM CourseSave cs " +
            "WHERE cs.memberId = :memberId AND cs.courseId IN :courseIds")
    List<Long> findSavedCourseIds(@Param("memberId") Long memberId,
                                  @Param("courseIds") Collection<Long> courseIds);
}
