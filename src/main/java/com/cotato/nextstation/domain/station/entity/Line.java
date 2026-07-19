package com.cotato.nextstation.domain.station.entity;

import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "line")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Line extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    public static Line of(String name) {
        return new Line(name);
    }

    private Line(String name) {
        this.name = name;
    }
}
