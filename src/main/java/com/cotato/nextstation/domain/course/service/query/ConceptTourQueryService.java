package com.cotato.nextstation.domain.course.service.query;

import com.cotato.nextstation.domain.course.dto.response.ConceptTourResponse;
import com.cotato.nextstation.domain.course.repository.ConceptTourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 컨셉별 투어 조회 전용 서비스.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConceptTourQueryService {

    private final ConceptTourRepository conceptTourRepository;

    /**
     * 컨셉별 투어 목록. 관리자가 정한 표시 순서대로 전부 내려준다.
     * <p>
     * 컨셉은 여덟 개 남짓이라 페이징하지 않는다. 화면의 검색창도 이 목록을 받아
     * 프론트에서 걸러내면 되므로 서버는 검색어를 받지 않는다.
     */
    public List<ConceptTourResponse> getConceptTours() {
        return conceptTourRepository.findAllWithCourseCount().stream()
                .map(conceptTour -> new ConceptTourResponse(
                        conceptTour.getConceptTourId(),
                        conceptTour.getName(),
                        conceptTour.getDescription(),
                        conceptTour.getCourseCount()))
                .toList();
    }
}
