package com.cotato.nextstation.domain.stamp.entity;

import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 코스 완료 시(POST /courses/{courseId}/complete) 생성되는 완주 이력.
@Entity
@Table(
        name = "member_place_stamps",
        indexes = {
                @Index(name = "idx_member_stamp_member", columnList = "member_id"),
                @Index(name = "idx_member_stamp_member_course", columnList = "member_id, course_id")
        },
        uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_member_stamp_member_course",
                columnNames = {"member_id", "course_id"}
            )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberStamp extends BaseTimeEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    // 완주 시점의 코스 역을 스냅샷으로 저장한다. 코스가 삭제(@SQLRestriction)돼도
    // 이 스탬프로 조회하는 화면(내 여행일지 목록 등)에서 역 정보가 사라지지 않게 하기 위함.
    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Builder
    public MemberStamp(Long memberId, Long courseId, Long stationId) {
        this.memberId = memberId;
        this.courseId = courseId;
        this.stationId = stationId;
    }
}