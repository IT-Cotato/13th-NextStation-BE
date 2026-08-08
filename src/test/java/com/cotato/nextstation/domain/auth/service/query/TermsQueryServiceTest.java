package com.cotato.nextstation.domain.auth.service.query;

import com.cotato.nextstation.domain.auth.converter.TermsConverter;
import com.cotato.nextstation.domain.auth.dto.response.TermsResponse;
import com.cotato.nextstation.domain.auth.dto.response.TermsSummaryResponse;
import com.cotato.nextstation.domain.auth.entity.TermsConsent;
import com.cotato.nextstation.domain.auth.entity.TermsType;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.repository.TermsConsentRepository;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                .title("마케팅 정보 수신 동의").content("내용").version("v1.0").isRequired(false).build();
        List<TermsConsent> latestTerms = List.of(required, optional);
        List<TermsSummaryResponse> expected = List.of(
                new TermsSummaryResponse(1L, TermsType.SERVICE, "서비스 이용약관", "v1.0", true),
                new TermsSummaryResponse(2L, TermsType.MARKETING, "마케팅 정보 수신 동의", "v1.0", false)
        );
        given(termsConsentRepository.findAllLatestOrderByRequiredDescIdAsc()).willReturn(latestTerms);
        given(termsConverter.toSummaryResponses(latestTerms)).willReturn(expected);

        // when
        List<TermsSummaryResponse> result = termsQueryService.getLatestTerms();

        // then
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    @DisplayName("약관 단건 조회는 type에 매핑된 title의 최신 버전을 원문까지 반환한다")
    void getLatestTerms_byType_success() {
        // given
        TermsConsent terms = TermsConsent.builder()
                .title("서비스 이용약관").content("내용").version("v1.0").isRequired(true).build();
        TermsResponse expected = new TermsResponse(1L, TermsType.SERVICE, "서비스 이용약관", "내용", "v1.0", true);
        given(termsConsentRepository.findFirstByTitleOrderByIdDesc("서비스 이용약관")).willReturn(Optional.of(terms));
        given(termsConverter.toResponse(terms)).willReturn(expected);

        // when
        TermsResponse result = termsQueryService.getLatestTerms(TermsType.SERVICE);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("아직 시딩되지 않은 약관 종류면 예외가 발생한다")
    void getLatestTerms_byType_notFound() {
        // given
        given(termsConsentRepository.findFirstByTitleOrderByIdDesc(TermsType.PRIVACY.getTitle()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> termsQueryService.getLatestTerms(TermsType.PRIVACY))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.TERMS_NOT_FOUND.getMessage());
    }
}
