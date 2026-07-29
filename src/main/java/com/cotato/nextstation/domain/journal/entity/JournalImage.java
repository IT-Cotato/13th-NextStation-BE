package com.cotato.nextstation.domain.journal.entity;

import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "journal_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalImage extends BaseTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private Journal journal;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Builder
    public JournalImage(Journal journal, String imageUrl) {
        this.journal = journal;
        this.imageUrl = imageUrl;
    }
}