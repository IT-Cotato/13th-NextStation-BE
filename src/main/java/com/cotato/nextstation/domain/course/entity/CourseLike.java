package com.cotato.nextstation.domain.course.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 코스 좋아요. 원본 코스를 참조만 하므로 원본이 삭제/비공개되면 목록에서도 빠진다.
// "내 코스로 만들기"와는 다른 개념이다.
@Entity
@Getter
@Table(
        name = "course_like",
        // 동시 요청으로 같은 코스가 두 번 좋아요되는 것을 DB에서 차단한다
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_like_member_course",
                columnNames = {"member_id", "course_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class CourseLike extends BaseEntity {

    // 연관관계 매핑 대신 FK 식별자만 보관한다.
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    // 좋아요 목록을 최근 좋아요순으로 정렬하기 위해 둔다.
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private CourseLike(Long memberId, Long courseId) {
        this.memberId = memberId;
        this.courseId = courseId;
    }
}
