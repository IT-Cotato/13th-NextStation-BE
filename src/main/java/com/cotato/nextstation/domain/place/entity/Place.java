package com.cotato.nextstation.domain.place.entity;

import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

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


    @Builder
    public Place(Long stationId,  Category category, String description, String placeName, String address,
                 String contactNumber, Long xCoordinate, Long yCoordinate) {
        this.stationId = stationId;
        this.category = category;
        this.description = description;
        this.placeName = placeName;
        this.address = address;
        this.contactNumber = contactNumber;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }
}