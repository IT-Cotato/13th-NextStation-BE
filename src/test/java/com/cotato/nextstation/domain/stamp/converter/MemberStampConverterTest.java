package com.cotato.nextstation.domain.stamp.converter;

import com.cotato.nextstation.domain.stamp.dto.response.MyStampDetailResponse;
import com.cotato.nextstation.domain.stamp.dto.response.MyStampListResponse;
import com.cotato.nextstation.domain.stamp.dto.response.StampResponse;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository.MyStampDetailView;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository.VisitedStationView;
import com.cotato.nextstation.domain.station.entity.LineCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class MemberStampConverterTest {

    private final MemberStampConverter memberStampConverter = new MemberStampConverter();

    @Test
    @DisplayName("스탬프 목록과 총 개수를 응답으로 변환한다")
    void toMyStampListResponse_success() {
        // given
        VisitedStationView view = mock(VisitedStationView.class);
        given(view.getStationId()).willReturn(5L);
        given(view.getStationName()).willReturn("제기동역");
        given(view.getLineId()).willReturn(1L);
        given(view.getLineName()).willReturn("1호선");
        given(view.getLineCode()).willReturn(LineCode.LINE_1);

        // when
        MyStampListResponse response = memberStampConverter.toMyStampListResponse(List.of(view));

        // then
        assertThat(response.totalCount()).isEqualTo(1);
        StampResponse stamp = response.stamps().get(0);
        assertThat(stamp.stationId()).isEqualTo(5L);
        assertThat(stamp.stationName()).isEqualTo("제기동역");
        assertThat(stamp.line().id()).isEqualTo(1L);
        assertThat(stamp.line().name()).isEqualTo("1호선");
        assertThat(stamp.line().code()).isEqualTo(LineCode.LINE_1);
    }

    @Test
    @DisplayName("대표 호선이 없으면 line이 null로 반환된다")
    void toMyStampListResponse_nullLine() {
        // given
        VisitedStationView view = mock(VisitedStationView.class);
        given(view.getStationId()).willReturn(7L);
        given(view.getStationName()).willReturn("동묘앞역");
        given(view.getLineId()).willReturn(null);

        // when
        MyStampListResponse response = memberStampConverter.toMyStampListResponse(List.of(view));

        // then
        assertThat(response.stamps().get(0).line()).isNull();
    }

    @Test
    @DisplayName("빈 목록이면 totalCount 0에 빈 stamps를 반환한다")
    void toMyStampListResponse_empty() {
        // when
        MyStampListResponse response = memberStampConverter.toMyStampListResponse(List.of());

        // then
        assertThat(response.totalCount()).isZero();
        assertThat(response.stamps()).isEmpty();
    }

    @Test
    @DisplayName("스탬프 상세를 역/노선/획득일/여행일지 id로 변환한다")
    void toMyStampDetailResponse_success() {
        // given
        MyStampDetailView view = mock(MyStampDetailView.class);
        given(view.getStationId()).willReturn(5L);
        given(view.getStationName()).willReturn("제기동역");
        given(view.getLineId()).willReturn(1L);
        given(view.getLineName()).willReturn("1호선");
        given(view.getLineCode()).willReturn(LineCode.LINE_1);
        given(view.getAcquiredAt()).willReturn(LocalDateTime.of(2026, 3, 2, 14, 20));

        // when
        MyStampDetailResponse response = memberStampConverter.toMyStampDetailResponse(view, 42L);

        // then
        assertThat(response.stationId()).isEqualTo(5L);
        assertThat(response.stationName()).isEqualTo("제기동역");
        assertThat(response.line().id()).isEqualTo(1L);
        assertThat(response.acquiredAt()).isEqualTo(LocalDateTime.of(2026, 3, 2, 14, 20));
        assertThat(response.journalId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("연결된 여행일지가 없으면 journalId가 null로 반환된다")
    void toMyStampDetailResponse_nullJournalId() {
        // given
        MyStampDetailView view = mock(MyStampDetailView.class);
        given(view.getStationId()).willReturn(5L);
        given(view.getStationName()).willReturn("제기동역");
        given(view.getLineId()).willReturn(null);
        given(view.getAcquiredAt()).willReturn(LocalDateTime.of(2026, 3, 2, 14, 20));

        // when
        MyStampDetailResponse response = memberStampConverter.toMyStampDetailResponse(view, null);

        // then
        assertThat(response.line()).isNull();
        assertThat(response.journalId()).isNull();
    }
}
