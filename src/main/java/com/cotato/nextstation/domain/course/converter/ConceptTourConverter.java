package com.cotato.nextstation.domain.course.converter;

import com.cotato.nextstation.domain.course.dto.response.ConceptTourResponse;
import com.cotato.nextstation.domain.course.repository.ConceptTourRepository.ConceptTourView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConceptTourConverter {

    public List<ConceptTourResponse> toResponses(List<ConceptTourView> conceptTours) {
        return conceptTours.stream()
                .map(conceptTour -> new ConceptTourResponse(
                        conceptTour.getConceptTourId(),
                        conceptTour.getName(),
                        conceptTour.getDescription(),
                        conceptTour.getCourseCount()))
                .toList();
    }
}
