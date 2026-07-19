package com.cotato.nextstation.domain.auth.entity;

import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "terms_consents",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_terms_consents_title_version",
                columnNames = {"title", "version"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermsConsent extends BaseTimeEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String version;

    @Column(name = "is_required", nullable = false)
    private boolean isRequired;

    @Builder
    private TermsConsent(String title, String content, String version, boolean isRequired) {
        this.title = title;
        this.content = content;
        this.version = version;
        this.isRequired = isRequired;
    }
}