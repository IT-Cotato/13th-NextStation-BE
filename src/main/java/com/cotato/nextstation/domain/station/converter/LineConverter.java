package com.cotato.nextstation.domain.station.converter;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.Line;
import com.cotato.nextstation.domain.station.repository.StationLineRepository.StationLineView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LineConverter {

    public LineSummaryResponse toSummaryResponse(Line line) {
        return new LineSummaryResponse(line.getId(), line.getName(), line.getCode());
    }

    // 역-호선 매핑 조회 결과(Line 엔티티를 따로 로딩하지 않는 프로젝션)용
    public LineSummaryResponse toSummaryResponse(StationLineView view) {
        return new LineSummaryResponse(view.getLineId(), view.getLineName(), view.getLineCode());
    }

    public List<LineSummaryResponse> toSummaryResponses(List<Line> lines) {
        return lines.stream()
                .map(this::toSummaryResponse)
                .toList();
    }
}