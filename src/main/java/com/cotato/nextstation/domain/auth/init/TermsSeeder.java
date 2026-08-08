package com.cotato.nextstation.domain.auth.init;

import com.cotato.nextstation.domain.auth.entity.TermsConsent;
import com.cotato.nextstation.domain.auth.entity.TermsType;
import com.cotato.nextstation.domain.auth.repository.TermsConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 약관 원문을 resources/data/terms/*.md 에서 읽어 terms_consents에 시딩한다.
 * (title, version)으로 찾아 내용이 바뀐 경우에만 UPDATE하고 row를 지우지 않으므로 member_terms_agreement가 FK로 참조 중이어도 재기동에 안전하다.
 * 약관은 변경 빈도가 매우 낮고 법적 문서라, 배포마다 운영 DB 문구가 자동으로 덮이면 안 되므로, 운영 반영은 수동으로 한다.
 */
@Slf4j
@Component
@Profile("!prod")
@Order(0)
@RequiredArgsConstructor
public class TermsSeeder implements ApplicationRunner {

    private static final String SEED_VERSION = "v1.0";
    // title은 TermsType이 갖고 있다. 조회 API가 type -> title로 약관을 찾으므로 여기서 문자열을 따로 쓰지 않는다.
    private static final List<TermsSeed> SEEDS = List.of(
            new TermsSeed(TermsType.SERVICE, "data/terms/service-v1.0.md", true),
            new TermsSeed(TermsType.PRIVACY, "data/terms/privacy-v1.0.md", true),
            new TermsSeed(TermsType.MARKETING, "data/terms/marketing-v1.0.md", false)
    );

    private final TermsConsentRepository termsConsentRepository;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        for (TermsSeed seed : SEEDS) {
            seed(seed, readContent(seed.path()));
        }
    }

    private void seed(TermsSeed seed, String content) {
        TermsConsent existing = termsConsentRepository.findByTitleAndVersion(seed.title(), SEED_VERSION)
                .orElse(null);

        if (existing == null) {
            termsConsentRepository.save(TermsConsent.builder()
                    .title(seed.title())
                    .content(content)
                    .version(SEED_VERSION)
                    .isRequired(seed.isRequired())
                    .build());
            log.info("약관 시딩: 신규 등록 title={}, version={}", seed.title(), SEED_VERSION);
            return;
        }

        if (existing.getContent().equals(content)) {
            log.info("약관 시딩: 내용 동일해 건너뜀 title={}, version={}", seed.title(), SEED_VERSION);
            return;
        }

        existing.updateContent(content);
        termsConsentRepository.save(existing);
        log.info("약관 시딩: 내용 갱신 title={}, version={}", seed.title(), SEED_VERSION);
    }

    private String readContent(String path) throws IOException {
        return StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
    }

    private record TermsSeed(TermsType type, String path, boolean isRequired) {

        String title() {
            return type.getTitle();
        }
    }
}