package com.cotato.nextstation.domain.stamp.service.query;

import com.cotato.nextstation.domain.member.exception.MemberErrorCode;
import com.cotato.nextstation.domain.member.service.query.MemberExistenceQueryService;
import com.cotato.nextstation.domain.stamp.dto.response.MemberStampListResponse;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.entity.LineCode;
import com.cotato.nextstation.domain.station.service.query.StationQueryService;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberStampQueryServiceTest {

    @InjectMocks
    private MemberStampQueryService memberStampQueryService;

    @Mock
    private MemberStampRepository memberStampRepository;

    @Mock
    private MemberExistenceQueryService memberExistenceQueryService;

    @Mock
    private StationQueryService stationQueryService;

    @Test
    @DisplayName("완료한 코스 id만 집합으로 반환한다")
    void getCompletedCourseIds_returnsOnlyCompleted() {
        // given: 3개를 물어봤는데 2개만 완료된 상태
        given(memberStampRepository.findCompletedCourseIds(1L, List.of(10L, 20L, 30L)))
                .willReturn(List.of(10L, 30L));

        // when
        Set<Long> completed = memberStampQueryService.getCompletedCourseIds(1L, List.of(10L, 20L, 30L));

        // then
        assertThat(completed).containsExactlyInAnyOrder(10L, 30L);
    }

    @Test
    @DisplayName("완료한 코스가 없으면 빈 집합을 반환한다")
    void getCompletedCourseIds_noneCompleted() {
        // given
        given(memberStampRepository.findCompletedCourseIds(1L, List.of(10L))).willReturn(List.of());

        // when
        Set<Long> completed = memberStampQueryService.getCompletedCourseIds(1L, List.of(10L));

        // then
        assertThat(completed).isEmpty();
    }

    @Test
    @DisplayName("코스 목록이 비어 있으면 조회하지 않고 빈 집합을 반환한다")
    void getCompletedCourseIds_emptyInputSkipsQuery() {
        // when: 코스가 없는 페이지에서 IN () 쿼리를 날릴 이유가 없다
        Set<Long> completed = memberStampQueryService.getCompletedCourseIds(1L, List.of());

        // then
        assertThat(completed).isEmpty();
        verify(memberStampRepository, never()).findCompletedCourseIds(anyLong(), any());
    }

    @Test
    @DisplayName("방문한 서로 다른 역의 개수를 그대로 반환한다")
    void getStampCount_returnsVisitedStationCount() {
        // given
        given(memberStampRepository.countVisitedStations(1L)).willReturn(12L);

        // when
        long stampCount = memberStampQueryService.getStampCount(1L);

        // then
        assertThat(stampCount).isEqualTo(12L);
    }

    @Test
    @DisplayName("존재하는 회원이면 최근 방문순 역 목록을 스탬프 목록으로 반환한다")
    void getMemberStamps_success() {
        // given
        StationSummaryResponse station = new StationSummaryResponse(6L, "보문역",
                List.of(new LineSummaryResponse(6L, "6호선", LineCode.LINE_6)));
        given(memberExistenceQueryService.existsMember(2L)).willReturn(true);
        given(memberStampRepository.findVisitedStationIdsOrderByLastVisitedDesc(2L)).willReturn(List.of(6L));
        given(stationQueryService.getStationSummaries(List.of(6L))).willReturn(List.of(station));

        // when
        MemberStampListResponse response = memberStampQueryService.getMemberStamps(2L);

        // then
        assertThat(response.stampCount()).isEqualTo(1);
        assertThat(response.stamps()).containsExactly(station);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 스탬프 목록을 조회하면 예외가 발생한다")
    void getMemberStamps_memberNotFound() {
        // given
        given(memberExistenceQueryService.existsMember(2L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> memberStampQueryService.getMemberStamps(2L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
    }
}
