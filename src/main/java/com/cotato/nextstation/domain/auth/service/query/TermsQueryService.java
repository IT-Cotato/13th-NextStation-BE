package com.cotato.nextstation.domain.auth.service.query;

import com.cotato.nextstation.domain.auth.converter.TermsConverter;
import com.cotato.nextstation.domain.auth.dto.response.TermsResponse;
import com.cotato.nextstation.domain.auth.repository.TermsConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsQueryService {

    private final TermsConsentRepository termsConsentRepository;
    private final TermsConverter termsConverter;

    public List<TermsResponse> getLatestTerms() {
        return termsConverter.toResponses(termsConsentRepository.findAllLatestOrderByRequiredDescIdAsc());
    }
}
