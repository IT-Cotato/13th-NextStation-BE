package com.cotato.nextstation.domain.place.repository;

import com.cotato.nextstation.domain.place.entity.PlaceTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceTagRepository extends JpaRepository<PlaceTag, Long> {
}