package com.cotato.nextstation.domain.place.entity;

import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "place")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseTimeEntity {

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(nullable = false)
    private String description;

    @Column(name = "place_name", nullable = false)
    private String placeName;

    @Column(nullable = false)
    private String address;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "x_coordinate", nullable = false)
    private Long xCoordinate;

    @Column(name = "y_coordinate", nullable = false)
    private Long yCoordinate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaceCategory category;

    @Builder
    public Place(Long stationId, String description, String placeName, String address,
                 String contactNumber, Long xCoordinate, Long yCoordinate, PlaceCategory category) {
        this.stationId = stationId;
        this.description = description;
        this.placeName = placeName;
        this.address = address;
        this.contactNumber = contactNumber;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        this.category = category;
    }
}