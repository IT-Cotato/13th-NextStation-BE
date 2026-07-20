package com.cotato.nextstation.domain.place.entity;

import com.cotato.nextstation.domain.place.enums.PlaceTagName;
import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "place_tag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceTag extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private PlaceTagName name;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public static PlaceTag of(PlaceTagName name, boolean isActive) {
        return new PlaceTag(name, isActive);
    }

    private PlaceTag(PlaceTagName name, boolean isActive) {
        this.name = name;
        this.isActive = isActive;
    }
}