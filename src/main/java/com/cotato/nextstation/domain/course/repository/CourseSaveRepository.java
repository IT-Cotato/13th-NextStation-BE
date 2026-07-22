package com.cotato.nextstation.domain.course.repository;

import com.cotato.nextstation.domain.course.entity.CourseSave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CourseSaveRepository extends JpaRepository<CourseSave, Long> {

    boolean existsByMemberIdAndCourseId(Long memberId, Long courseId);

    // 여러 코스의 스크랩 여부를 한 번에 조회
    @Query("SELECT cs.courseId FROM CourseSave cs " +
            "WHERE cs.memberId = :memberId AND cs.courseId IN :courseIds")
    List<Long> findSavedCourseIds(@Param("memberId") Long memberId,
                                  @Param("courseIds") Collection<Long> courseIds);

    // 스크랩을 삭제하고 실제로 지워진 행 수를 돌려준다.
    // 조회 후 삭제하면 동시 취소 시 두 요청이 모두 "있다"고 보고 저장 수를 각각 줄이게 되므로,
    // 삭제 쿼리의 영향 행 수를 확인 수단으로 삼아 실제로 지운 요청만 저장 수를 줄인다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CourseSave cs " +
            "WHERE cs.memberId = :memberId AND cs.courseId = :courseId")
    int deleteByMemberIdAndCourseId(@Param("memberId") Long memberId,
                                    @Param("courseId") Long courseId);
}
