package com.cotato.nextstation.domain.course.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// order_num은 재정렬 중 값이 교환되며 일시적으로 겹칠 수 있어 유니크 제약을 두지 않는다.
@Entity
@Getter
@Table(
        name = "course_places",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_places_course_place",
                columnNames = {"course_id", "place_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoursePlace extends BaseEntity {

    // 연관관계 매핑 대신 FK 식별자(Long)만 보관한다.
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "order_num", nullable = false)
    private int orderNum;

    @Builder
    private CoursePlace(Long courseId, Long placeId, int orderNum) {
        this.courseId = courseId;
        this.placeId = placeId;
        this.orderNum = orderNum;
    }

    public void updateOrderNum(int orderNum) {
        this.orderNum = orderNum;
    }
}
