package com.cotato.nextstation.domain.auth.util;

import com.cotato.nextstation.domain.auth.entity.TermsConsent;
import com.cotato.nextstation.domain.auth.exception.TermsErrorCode;
import com.cotato.nextstation.domain.auth.repository.TermsConsentRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// SignupCommandService(이메일)/KakaoSignupCommandService(카카오)가 공유하는 약관 동의 검증
@Slf4j
@Component
@RequiredArgsConstructor
public class TermsAgreementValidator {

    private final TermsConsentRepository termsConsentRepository;

    public void validate(List<Long> agreedTermsIds) {
        List<TermsConsent> latestTerms = termsConsentRepository.findAllLatestOrderByRequiredDescIdAsc();

        Set<Long> latestTermsIds = latestTerms.stream()
                .map(TermsConsent::getId)
                .collect(Collectors.toSet());
        if (!latestTermsIds.containsAll(agreedTermsIds)) {
            log.warn("존재하지 않는 약관 id 포함: agreedTermsIds={}", agreedTermsIds);
            throw new CustomException(TermsErrorCode.TERMS_NOT_FOUND);
        }

        Set<Long> requiredTermsIds = latestTerms.stream()
                .filter(TermsConsent::isRequired)
                .map(TermsConsent::getId)
                .collect(Collectors.toSet());
        if (!agreedTermsIds.containsAll(requiredTermsIds)) {
            log.warn("필수 약관 미동의: requiredTermsIds={}, agreedTermsIds={}", requiredTermsIds, agreedTermsIds);
            throw new CustomException(TermsErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }
    }
}