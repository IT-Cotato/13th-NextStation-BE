package com.cotato.nextstation.domain.stamp.repository;

import com.cotato.nextstation.domain.stamp.entity.MemberStamp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface MemberStampRepository extends JpaRepository<MemberStamp, Long> {

    boolean existsByMemberIdAndId(Long memberId, Long id);
    boolean existsByMemberIdAndCourseId(Long memberId, Long courseId);


    // 넘긴 코스들 중 해당 회원이 완료한 코스 id만 반환한다.
    // 코스 목록은 한 페이지에 여러 건이라 코스마다 단건 조회하면 쿼리가 그만큼 나간다.
    @Query("SELECT ms.courseId FROM MemberStamp ms " +
            "WHERE ms.memberId = :memberId AND ms.courseId IN :courseIds")
    List<Long> findCompletedCourseIds(@Param("memberId") Long memberId,
                                      @Param("courseIds") List<Long> courseIds);

    @Query("SELECT ms FROM MemberStamp ms " +
            "WHERE ms.memberId = :memberId " +
            "AND NOT EXISTS (" +
            "    SELECT j FROM Journal j " +
            "    WHERE j.memberStampId = ms.id" +
            ") " +
            "ORDER BY ms.createdAt DESC")
    List<MemberStamp> findUncompletedByMemberId(@Param("memberId") Long memberId);

    List<MemberStamp> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    // 완료된 스탬프 제외하고 미작성 스탬프만 조회 (최신순)
    List<MemberStamp> findByMemberIdAndIdNotInOrderByCreatedAtDesc(
            Long memberId, Set<Long> completedStampIds);

}
