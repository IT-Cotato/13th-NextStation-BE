package com.cotato.nextstation.domain.station.converter;

import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.entity.Station;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StationConverter {

    public StationSummaryResponse toSummaryResponse(Station station, List<String> lines) {
        return new StationSummaryResponse(station.getId(), station.getStationName(), lines);
    }
}
