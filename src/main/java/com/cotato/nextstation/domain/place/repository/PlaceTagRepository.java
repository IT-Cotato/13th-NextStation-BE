package com.cotato.nextstation.domain.place.repository;

import com.cotato.nextstation.domain.place.entity.PlaceTag;
import com.cotato.nextstation.domain.place.enums.PlaceTagName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceTagRepository extends JpaRepository<PlaceTag, Long> {

    Optional<PlaceTag> findByName(PlaceTagName name);
}