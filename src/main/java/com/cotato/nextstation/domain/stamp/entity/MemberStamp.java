package com.cotato.nextstation.domain.stamp.entity;

import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 코스 완료 시(POST /courses/{courseId}/complete) 생성되는 완주 이력.
@Entity
@Table(name = "member_place_stamps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberStamp extends BaseTimeEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Builder
    public MemberStamp(Long memberId, Long courseId) {
        this.memberId = memberId;
        this.courseId = courseId;
    }
}