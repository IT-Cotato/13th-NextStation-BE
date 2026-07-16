package com.cotato.nextstation.domain.place.entity;

import com.cotato.nextstation.domain.place.enums.CategoryCode;
import com.cotato.nextstation.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// 장소 카테고리 데이터 (CAFE/FOOD/CULTURE/WALK).
// defaultImageUrl: 장소에 등록된 이미지가 없을 때 노출할 카테고리별 기본 이미지.
@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private CategoryCode code;

    @Column(nullable = false)
    private String name;

    @Column(name = "default_image_url")
    private String defaultImageUrl;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static Category of(CategoryCode code, String name, String defaultImageUrl) {
        return new Category(code, name, defaultImageUrl);
    }

    private Category(CategoryCode code, String name, String defaultImageUrl) {
        this.code = code;
        this.name = name;
        this.defaultImageUrl = defaultImageUrl;
    }

}