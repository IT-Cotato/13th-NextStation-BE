package com.cotato.nextstation.domain.auth.init;

import com.cotato.nextstation.domain.auth.entity.TermsConsent;
import com.cotato.nextstation.domain.auth.repository.TermsConsentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TermsSeederTest {

    private static final String SERVICE_TITLE = "서비스 이용약관";
    private static final String PRIVACY_TITLE = "개인정보 수집 및 이용 동의";
    private static final String MARKETING_TITLE = "마케팅 정보 수신 동의";
    private static final String VERSION = "v1.0";

    @InjectMocks
    private TermsSeeder termsSeeder;

    @Mock
    private TermsConsentRepository termsConsentRepository;

    @Test
    @DisplayName("DB에 없는 약관은 md 원문을 읽어 신규 등록한다")
    void seed_insertsWhenAbsent() throws IOException {
        // given
        given(termsConsentRepository.findByTitleAndVersion(anyString(), anyString())).willReturn(Optional.empty());

        // when
        termsSeeder.run(null);

        // then
        ArgumentCaptor<TermsConsent> captor = ArgumentCaptor.forClass(TermsConsent.class);
        verify(termsConsentRepository, times(3)).save(captor.capture());

        List<TermsConsent> saved = captor.getAllValues();
        assertThat(saved).extracting(TermsConsent::getTitle)
                .containsExactly(SERVICE_TITLE, PRIVACY_TITLE, MARKETING_TITLE);
        assertThat(saved).extracting(TermsConsent::isRequired).containsExactly(true, true, false);
        // 클래스패스의 md 경로가 실제로 존재하고 원문이 통째로 읽혔는지 확인
        assertThat(saved.get(0).getContent())
                .startsWith("# 환승여행 서비스 이용약관")
                .contains("제15조 분쟁 해결 및 기타 사항");
        assertThat(saved.get(1).getContent())
                .startsWith("# 환승여행 개인정보처리방침")
                .contains("10. 개인정보처리방침의 변경");
        assertThat(saved.get(2).getContent())
                .startsWith("# 환승여행 마케팅 정보 수신 동의")
                .contains("동의 철회 또는 회원 탈퇴 시까지");
    }

    @Test
    @DisplayName("내용이 md 원문과 같으면 저장하지 않는다")
    void seed_skipsWhenContentUnchanged() throws IOException {
        // given
        given(termsConsentRepository.findByTitleAndVersion(anyString(), anyString()))
                .willAnswer(invocation -> {
                    String title = invocation.getArgument(0);
                    return Optional.of(TermsConsent.builder()
                            .title(title)
                            .content(readMd(title))
                            .version(VERSION)
                            .isRequired(true)
                            .build());
                });

        // when
        termsSeeder.run(null);

        // then
        verify(termsConsentRepository, never()).save(any());
    }

    @Test
    @DisplayName("md 원문이 바뀌면 기존 row의 내용만 갱신한다")
    void seed_updatesWhenContentChanged() throws IOException {
        // given
        TermsConsent stale = TermsConsent.builder()
                .title(SERVICE_TITLE).content("제1조 (목적) ...").version(VERSION).isRequired(true).build();
        given(termsConsentRepository.findByTitleAndVersion(SERVICE_TITLE, VERSION)).willReturn(Optional.of(stale));
        given(termsConsentRepository.findByTitleAndVersion(PRIVACY_TITLE, VERSION)).willReturn(Optional.empty());
        given(termsConsentRepository.findByTitleAndVersion(MARKETING_TITLE, VERSION)).willReturn(Optional.empty());

        // when
        termsSeeder.run(null);

        // then
        assertThat(stale.getContent()).startsWith("# 환승여행 서비스 이용약관");
        verify(termsConsentRepository, times(1)).save(stale);
    }

    private String readMd(String title) throws IOException {
        String path = switch (title) {
            case SERVICE_TITLE -> "data/terms/service-v1.0.md";
            case PRIVACY_TITLE -> "data/terms/privacy-v1.0.md";
            default -> "data/terms/marketing-v1.0.md";
        };
        return StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
    }
}