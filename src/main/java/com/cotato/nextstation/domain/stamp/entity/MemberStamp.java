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

    // 완료 시점의 역을 스냅샷으로 저장한다. 코스는 이후 삭제(소프트 삭제)될 수 있는데,
    // Course를 조인해 역을 조회하면 Course의 @SQLRestriction 때문에 완주 이력(스탬프)까지
    // 함께 사라져 보인다. 완주 이력은 코스 삭제와 무관하게 영구 보존돼야 하므로 여기 따로 둔다.
    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Builder
    public MemberStamp(Long memberId, Long courseId, Long stationId) {
        this.memberId = memberId;
        this.courseId = courseId;
        this.stationId = stationId;
    }
}