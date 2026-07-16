package com.cotato.nextstation.domain.course.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "course_places")
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
