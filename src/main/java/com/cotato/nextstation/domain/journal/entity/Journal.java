package com.cotato.nextstation.domain.journal.entity;

import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Journal extends BaseTimeEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Journal(Long memberId, boolean isPublic) {
        this.memberId = memberId;
        this.isPublic = isPublic;
        this.isDeleted = false;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}