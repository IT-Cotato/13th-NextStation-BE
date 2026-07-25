package com.cotato.nextstation.domain.course.entity;

import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "course")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseTimeEntity {

    // 연관관계 매핑 대신 FK 식별자(Long)만 보관한다.
    @Column(name = "original_course_id")
    private Long originalCourseId;

    @Column(name = "journal_id")
    private Long journalId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "concept_tour_id")
    private Long conceptTourId;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "save_count", nullable = false)
    private int saveCount;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Builder
    private Course(Long memberId, Long stationId, String name, Long originalCourseId) {
        this.memberId = memberId;
        this.stationId = stationId;
        this.name = name;
        // 원본 없이 새로 만든 코스는 null이고, "내 코스로 만들기"로 복제한 코스만 원본을 가리킨다.
        this.originalCourseId = originalCourseId;
        this.viewCount = 0;
        this.saveCount = 0;
        this.isDeleted = false;
    }

    public void updateName(String name) {
        this.name = name;
    }

    // soft delete. @SQLRestriction("is_deleted = false")로 이후 조회에서 자동 제외된다.
    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
