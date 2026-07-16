package com.cotato.nextstation.domain.place.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Place와 PlaceTag의 N:M 관계를 풀어주는 매핑 테이블.
@Entity
@Table(
        name = "place_tag_mapping",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_place_tag_mapping_place_tag",
                columnNames = {"place_id", "place_tag_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceTagMapping extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_tag_id", nullable = false)
    private PlaceTag placeTag;

    public static PlaceTagMapping of(Place place, PlaceTag placeTag) {
        return new PlaceTagMapping(place, placeTag);
    }

    private PlaceTagMapping(Place place, PlaceTag placeTag) {
        this.place = place;
        this.placeTag = placeTag;
    }
}