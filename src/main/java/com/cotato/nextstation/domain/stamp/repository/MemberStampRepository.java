package com.cotato.nextstation.domain.stamp.repository;

import com.cotato.nextstation.domain.stamp.entity.MemberStamp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberStampRepository extends JpaRepository<MemberStamp, Long> {
}