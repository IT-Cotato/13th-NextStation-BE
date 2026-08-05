package com.cotato.nextstation.domain.auth.service.query;

import com.cotato.nextstation.domain.auth.converter.TermsConverter;
import com.cotato.nextstation.domain.auth.dto.response.TermsResponse;
import com.cotato.nextstation.domain.auth.dto.response.TermsSummaryResponse;
import com.cotato.nextstation.domain.auth.entity.TermsConsent;
import com.cotato.nextstation.domain.auth.entity.TermsType;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.repository.TermsConsentRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsQueryService {

    private final TermsConsentRepository termsConsentRepository;
    private final TermsConverter termsConverter;

    public List<TermsSummaryResponse> getLatestTerms() {
        return termsConverter.toSummaryResponses(termsConsentRepository.findAllLatestOrderByRequiredDescIdAsc());
    }

    public TermsResponse getLatestTerms(TermsType type) {
        TermsConsent termsConsent = termsConsentRepository.findFirstByTitleOrderByIdDesc(type.getTitle())
                .orElseThrow(() -> {
                    log.warn("시딩되지 않은 약관 조회 시도: type={}", type);
                    return new CustomException(AuthErrorCode.TERMS_NOT_FOUND);
                });
        return termsConverter.toResponse(termsConsent);
    }
}