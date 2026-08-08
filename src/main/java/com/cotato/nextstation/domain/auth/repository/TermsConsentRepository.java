package com.cotato.nextstation.domain.auth.repository;

import com.cotato.nextstation.domain.auth.entity.TermsConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TermsConsentRepository extends JpaRepository<TermsConsent, Long> {

    Optional<TermsConsent> findByTitleAndVersion(String title, String version);

    // 같은 title 중 가장 최근에 추가된 row가 최신 버전 (findAllLatestOrderByRequiredDescIdAsc의 단건 버전)
    Optional<TermsConsent> findFirstByTitleOrderByIdDesc(String title);

    // 같은 title 중 가장 최근에 추가된 row만 최신 버전으로 간주 (version 관리)
    @Query("""
            SELECT t FROM TermsConsent t
            WHERE t.id IN (
                SELECT MAX(t2.id) FROM TermsConsent t2 GROUP BY t2.title
            )
            ORDER BY t.isRequired DESC, t.id ASC
            """)
    List<TermsConsent> findAllLatestOrderByRequiredDescIdAsc();
}