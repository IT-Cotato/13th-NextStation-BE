package com.cotato.nextstation.domain.auth.repository;

import com.cotato.nextstation.domain.auth.entity.MemberTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberTermsAgreementRepository extends JpaRepository<MemberTermsAgreement, Long> {
}