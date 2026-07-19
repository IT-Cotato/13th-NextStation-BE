package com.cotato.nextstation.domain.auth.converter;

import com.cotato.nextstation.domain.auth.dto.response.TermsResponse;
import com.cotato.nextstation.domain.auth.entity.TermsConsent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TermsConverter {

    public TermsResponse toResponse(TermsConsent termsConsent) {
        return new TermsResponse(
                termsConsent.getId(),
                termsConsent.getTitle(),
                termsConsent.getContent(),
                termsConsent.getVersion(),
                termsConsent.isRequired()
        );
    }

    public List<TermsResponse> toResponses(List<TermsConsent> termsConsents) {
        return termsConsents.stream()
                .map(this::toResponse)
                .toList();
    }
}
