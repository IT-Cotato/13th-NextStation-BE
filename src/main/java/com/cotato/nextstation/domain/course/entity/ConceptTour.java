package com.cotato.nextstation.domain.course.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 컨셉별 투어. "문구 투어", "가성비 투어"처럼 코스를 주제로 묶은 분류다.
 * <p>
 * 사용자가 코스를 만들 때 고르는 값이 아니라 **관리자가 나중에 큐레이션**해서 붙인다.
 * 그래서 모든 코스가 컨셉에 속하지는 않고, {@code course.concept_tour_id}는 nullable이다.
 * <p>
 * ERD에 updated_at이 없어 {@code BaseTimeEntity} 대신 {@code BaseEntity}를 상속하고
 * created_at만 직접 둔다. 관리자가 채우는 데이터라 수정 이력을 따로 남기지 않는다.
 */
@Entity
@Table(name = "concept_tour")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConceptTour extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 화면에 보여줄 순서. 인기·최신 같은 기준이 없어 관리자가 정한 순서를 그대로 따른다.
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ConceptTour(String name, String description, int displayOrder) {
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
    }
}
