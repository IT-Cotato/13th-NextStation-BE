package com.cotato.nextstation.domain.course.service.query;

import com.cotato.nextstation.domain.course.dto.request.ExploreCourseCondition;
import com.cotato.nextstation.domain.course.dto.response.ConceptTourResponse;
import com.cotato.nextstation.domain.course.dto.response.ExploreCourseListResponse;
import com.cotato.nextstation.domain.course.dto.response.ExploreResponse;
import com.cotato.nextstation.domain.course.entity.CourseSort;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.LineCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExploreQueryServiceTest {

    @InjectMocks
    private ExploreQueryService exploreQueryService;

    @Mock
    private CourseQueryService courseQueryService;

    @Mock
    private ConceptTourQueryService conceptTourQueryService;

    private ExploreCourseListResponse emptyList() {
        return new ExploreCourseListResponse(List.of(), null, false);
    }

    private ConceptTourResponse conceptTour(long id) {
        return new ConceptTourResponse(id, "컨셉" + id, "설명", 0);
    }

    @Test
    @DisplayName("각 섹션을 화면에 보이는 개수만큼만 조회한다")
    void getExplore_sectionSizes() {
        // given
        given(courseQueryService.getMostLikedCourses(isNull(), isNull(), eq(6))).willReturn(emptyList());
        given(conceptTourQueryService.getConceptTours())
                .willReturn(List.of(conceptTour(1), conceptTour(2), conceptTour(3), conceptTour(4)));
        given(courseQueryService.getExploreLines())
                .willReturn(List.of(new LineSummaryResponse(4L, "1호선", LineCode.LINE_1)));
        given(courseQueryService.getExploreCourses(isNull(), any(), eq(CourseSort.LATEST), isNull(), eq(3)))
                .willReturn(emptyList());

        // when
        ExploreResponse result = exploreQueryService.getExplore(null);

        // then: 컨셉은 3개까지만 자른다
        assertThat(result.conceptTours()).hasSize(3);
        verify(courseQueryService).getMostLikedCourses(null, null, 6);
        verify(courseQueryService).getExploreCourses(any(), any(), any(), any(), eq(3));
    }

    @Test
    @DisplayName("노선 목록의 첫 번째를 처음 선택된 노선으로 둔다")
    void getExplore_selectsFirstLine() {
        // given: 특정 호선을 고정하면 그 노선에 코스가 없을 때 빈 화면이 된다
        lenient().when(courseQueryService.getMostLikedCourses(any(), any(), any())).thenReturn(emptyList());
        lenient().when(conceptTourQueryService.getConceptTours()).thenReturn(List.of());
        given(courseQueryService.getExploreLines()).willReturn(List.of(
                new LineSummaryResponse(4L, "1호선", LineCode.LINE_1),
                new LineSummaryResponse(9L, "2호선", LineCode.LINE_2)));
        given(courseQueryService.getExploreCourses(any(), any(), any(), any(), any())).willReturn(emptyList());

        // when
        ExploreResponse result = exploreQueryService.getExplore(null);

        // then
        assertThat(result.selectedLineId()).isEqualTo(4L);
        verify(courseQueryService).getExploreCourses(
                null, new ExploreCourseCondition(4L, null, null, null), CourseSort.LATEST, null, 3);
    }

    @Test
    @DisplayName("노출할 노선이 없으면 선택 노선과 코스를 비운다")
    void getExplore_noLines() {
        // given: 공개 코스가 하나도 없는 초기 상태
        lenient().when(courseQueryService.getMostLikedCourses(any(), any(), any())).thenReturn(emptyList());
        lenient().when(conceptTourQueryService.getConceptTours()).thenReturn(List.of());
        given(courseQueryService.getExploreLines()).willReturn(List.of());

        // when
        ExploreResponse result = exploreQueryService.getExplore(null);

        // then: 노선이 없으면 코스 조회 자체를 하지 않는다
        assertThat(result.selectedLineId()).isNull();
        assertThat(result.lineCourses()).isEmpty();
        verify(courseQueryService, never()).getExploreCourses(any(), any(), any(), any(), any());
    }
}
