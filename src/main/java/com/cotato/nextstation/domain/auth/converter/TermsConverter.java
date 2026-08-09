package com.cotato.nextstation.domain.auth.converter;

import com.cotato.nextstation.domain.auth.dto.response.TermsResponse;
import com.cotato.nextstation.domain.auth.dto.response.TermsSummaryResponse;
import com.cotato.nextstation.domain.auth.entity.TermsConsent;
import com.cotato.nextstation.domain.auth.entity.TermsType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TermsConverter {

    /**
     * 원문 조회 응답. 화면이 문서 전문을 보여주므로 동의 항목 이름이 아니라 문서 제목을 내려준다.
     * 시더가 만들지 않은 약관은 종류를 알 수 없어 저장된 제목을 그대로 쓴다.
     */
    public TermsResponse toResponse(TermsConsent termsConsent) {
        TermsType type = TermsType.fromTitle(termsConsent.getTitle());
        return new TermsResponse(
                termsConsent.getId(),
                type,
                type == null ? termsConsent.getTitle() : type.getDocumentTitle(),
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