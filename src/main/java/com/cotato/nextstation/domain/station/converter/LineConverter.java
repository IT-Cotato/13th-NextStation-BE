package com.cotato.nextstation.domain.station.converter;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.Line;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LineConverter {

    public LineSummaryResponse toSummaryResponse(Line line) {
        return new LineSummaryResponse(line.getId(), line.getName(), line.getCode());
    }

    public List<LineSummaryResponse> toSummaryResponses(List<Line> lines) {
        return lines.stream()
                .map(this::toSummaryResponse)
                .toList();
    }
}