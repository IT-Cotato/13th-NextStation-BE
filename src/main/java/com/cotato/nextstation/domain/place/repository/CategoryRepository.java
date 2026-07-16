package com.cotato.nextstation.domain.place.repository;

import com.cotato.nextstation.domain.place.entity.Category;
import com.cotato.nextstation.domain.place.enums.CategoryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCode(CategoryCode code);
}