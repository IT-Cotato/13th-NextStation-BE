package com.cotato.nextstation.domain.auth.converter;

import com.cotato.nextstation.domain.auth.dto.response.TermsResponse;
import com.cotato.nextstation.domain.auth.dto.response.TermsSummaryResponse;
import com.cotato.nextstation.domain.auth.entity.TermsConsent;
import com.cotato.nextstation.domain.auth.entity.TermsType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TermsConverter {

    public TermsResponse toResponse(TermsConsent termsConsent) {
        return new TermsResponse(
                termsConsent.getId(),
                TermsType.fromTitle(termsConsent.getTitle()),
                termsConsent.getTitle(),
                termsConsent.getContent(),
                termsConsent.getVersion(),
                termsConsent.isRequired()
        );
    }

    public TermsSummaryResponse toSummaryResponse(TermsConsent termsConsent) {
        return new TermsSummaryResponse(
                termsConsent.getId(),
                TermsType.fromTitle(termsConsent.getTitle()),
                termsConsent.getTitle(),
                termsConsent.getVersion(),
                termsConsent.isRequired()
        );
    }

    public List<TermsSummaryResponse> toSummaryResponses(List<TermsConsent> termsConsents) {
        return termsConsents.stream()
                .map(this::toSummaryResponse)
                .toList();
    }
}