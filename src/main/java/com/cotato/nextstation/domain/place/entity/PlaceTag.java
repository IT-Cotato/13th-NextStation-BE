package com.cotato.nextstation.domain.place.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 장소에 부여 가능한 태그의 마스터 데이터.
// PlaceTagMapping을 통해 Place와 N:M으로 연결된다.
// isActive가 false면 신규 부여는 막되, 기존 매핑 데이터는 유지한다(운영 중 태그 비활성화 대비).
@Entity
@Table(name = "place_tag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceTag extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaceTagName name;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder
    public PlaceTag(PlaceTagName name) {
        this.name = name;
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}