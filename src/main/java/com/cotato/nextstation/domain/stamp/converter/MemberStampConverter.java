package com.cotato.nextstation.domain.stamp.converter;

import com.cotato.nextstation.domain.stamp.dto.response.MyStampListResponse;
import com.cotato.nextstation.domain.stamp.dto.response.StampResponse;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository.MyStampView;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemberStampConverter {

    public MyStampListResponse toMyStampListResponse(List<MyStampView> stamps) {
        List<StampResponse> responses = stamps.stream()
                .map(this::toStampResponse)
                .toList();
        return new MyStampListResponse(responses.size(), responses);
    }

    private StampResponse toStampResponse(MyStampView stamp) {
        LineSummaryResponse line = stamp.getLineId() == null
                ? null
                : new LineSummaryResponse(stamp.getLineId(), stamp.getLineName(), stamp.getLineCode());
        return new StampResponse(stamp.getStationId(), stamp.getStationName(), line);
    }
}
