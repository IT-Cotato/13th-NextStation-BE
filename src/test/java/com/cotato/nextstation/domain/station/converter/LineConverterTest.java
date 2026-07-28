package com.cotato.nextstation.domain.station.converter;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.Line;
import com.cotato.nextstation.domain.station.entity.LineCode;
import com.cotato.nextstation.domain.station.repository.StationLineRepository.StationLineView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class LineConverterTest {

    private final LineConverter lineConverter = new LineConverter();

    private Line line(Long id, LineCode code) {
        Line line = Line.of(code);
        ReflectionTestUtils.setField(line, "id", id);
        return line;
    }

    private StationLineView lineView(Long lineId, LineCode code) {
        StationLineView view = mock(StationLineView.class);
        given(view.getLineId()).willReturn(lineId);
        given(view.getLineName()).willReturn(code.getDisplayName());
        given(view.getLineCode()).willReturn(code);
        return view;
    }

    @Test
    @DisplayName("Line 엔티티의 id/name/code를 그대로 옮긴다")
    void toSummaryResponse_fromEntity() {
        // given
        Line line = line(6L, LineCode.LINE_6);

        // when
        LineSummaryResponse response = lineConverter.toSummaryResponse(line);

        // then
        assertThat(response.id()).isEqualTo(6L);
        assertThat(response.name()).isEqualTo("6호선");
        assertThat(response.code()).isEqualTo(LineCode.LINE_6);
    }

    @Test
    @DisplayName("역-호선 프로젝션의 lineId/lineName/lineCode를 그대로 옮긴다")
    void toSummaryResponse_fromProjection() {
        // given
        StationLineView view = lineView(18L, LineCode.UI_SINSEOL);

        // when
        LineSummaryResponse response = lineConverter.toSummaryResponse(view);

        // then
        assertThat(response.id()).isEqualTo(18L);
        assertThat(response.name()).isEqualTo("우이신설선");
        assertThat(response.code()).isEqualTo(LineCode.UI_SINSEOL);
    }

    @Test
    @DisplayName("여러 노선을 변환해도 입력 순서를 유지한다")
    void toSummaryResponses_keepsOrder() {
        // given: 노선 ID 순이 아닌 순서로 넣는다
        List<Line> lines = List.of(line(6L, LineCode.LINE_6), line(1L, LineCode.LINE_1));

        // when
        List<LineSummaryResponse> responses = lineConverter.toSummaryResponses(lines);

        // then
        assertThat(responses).extracting(LineSummaryResponse::name).containsExactly("6호선", "1호선");
    }

    @Test
    @DisplayName("빈 목록은 빈 목록으로 변환한다")
    void toSummaryResponses_empty() {
        // when
        List<LineSummaryResponse> responses = lineConverter.toSummaryResponses(List.of());

        // then
        assertThat(responses).isEmpty();
    }
}
