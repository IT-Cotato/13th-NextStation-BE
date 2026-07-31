package com.cotato.nextstation.domain.course.repository;

import com.cotato.nextstation.domain.course.entity.ConceptTour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ConceptTourRepository extends JpaRepository<ConceptTour, Long> {

    /**
     * 컨셉 목록과 각 컨셉에 속한 코스 수. 카드에 "코스 18개"로 표시한다.
     * <p>
     * 코스 수는 목록에 실제로 보이는 것과 같아야 하므로 둘러보기와 같은 공개 조건을 건다.
     * 컨셉에 코스가 하나도 없어도 카드는 보여야 해서 LEFT JOIN이다.
     * <p>
     * 컨셉마다 코스 수를 세면 컨셉 수만큼 쿼리가 나가서 한 번에 집계한다.
     */
    @Query("SELECT ct.id AS conceptTourId, ct.name AS name, ct.description AS description, " +
            "COUNT(c.id) AS courseCount " +
            "FROM ConceptTour ct " +
            "LEFT JOIN Course c ON c.conceptTourId = ct.id " +
            "     AND EXISTS (SELECT 1 FROM Journal j WHERE j.id = c.journalId AND j.isPublic = true) " +
            "GROUP BY ct.id, ct.name, ct.description, ct.displayOrder " +
            "ORDER BY ct.displayOrder ASC, ct.id ASC")
    List<ConceptTourView> findAllWithCourseCount();

    interface ConceptTourView {
        Long getConceptTourId();
        String getName();
        String getDescription();
        long getCourseCount();
    }
}
