package com.cotato.nextstation.domain.auth.service.query;

import com.cotato.nextstation.domain.auth.converter.TermsConverter;
import com.cotato.nextstation.domain.auth.dto.response.TermsResponse;
import com.cotato.nextstation.domain.auth.entity.TermsConsent;
import com.cotato.nextstation.domain.auth.repository.TermsConsentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TermsQueryServiceTest {

    @InjectMocks
    private TermsQueryService termsQueryService;

    @Mock
    private TermsConsentRepository termsConsentRepository;

    @Mock
    private TermsConverter termsConverter;

    @Test
    @DisplayName("최신 약관 목록을 Repository 정렬 순서 그대로 변환해 반환한다")
    void getLatestTerms_success() {
        // given
        TermsConsent required = TermsConsent.builder()
                .title("서비스 이용약관").content("내용").version("v1.0").isRequired(true).build();
        TermsConsent optional = TermsConsent.builder()
                .title("마케팅 수신 동의").content("내용").version("v1.0").isRequired(false).build();
        List<TermsConsent> latestTerms = List.of(required, optional);
        List<TermsResponse> expected = List.of(
                new TermsResponse(1L, "서비스 이용약관", "내용", "v1.0", true),
                new TermsResponse(2L, "마케팅 수신 동의", "내용", "v1.0", false)
        );
        given(termsConsentRepository.findAllLatestOrderByRequiredDescIdAsc()).willReturn(latestTerms);
        given(termsConverter.toResponses(latestTerms)).willReturn(expected);

        // when
        List<TermsResponse> result = termsQueryService.getLatestTerms();

        // then
        assertThat(result).containsExactlyElementsOf(expected);
    }
}
