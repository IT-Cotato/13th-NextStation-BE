package com.cotato.nextstation.domain.departure.repository;

import com.cotato.nextstation.domain.departure.entity.MemberDepartureStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberDepartureStationRepository extends JpaRepository<MemberDepartureStation, Long> {

    List<MemberDepartureStation> findByMemberIdOrderByOrderNumAsc(Long memberId);

    long countByMemberId(Long memberId);

    Optional<MemberDepartureStation> findByIdAndMemberId(Long id, Long memberId);

    @Query("SELECT COALESCE(MAX(d.orderNum), 0) FROM MemberDepartureStation d WHERE d.memberId = :memberId")
    int findMaxOrderNumByMemberId(@Param("memberId") Long memberId);
}
