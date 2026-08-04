package com.cotato.nextstation.domain.stamp.service.query;

import com.cotato.nextstation.domain.member.exception.MemberErrorCode;
import com.cotato.nextstation.domain.member.service.query.MemberExistenceQueryService;
import com.cotato.nextstation.domain.stamp.dto.response.MemberStampListResponse;
import com.cotato.nextstation.domain.stamp.dto.response.MemberStampResponse;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository.MemberStampView;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.entity.LineCode;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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

    private MemberStampView stampView(Long stationId, String stationName, Long lineId, String lineName, LineCode lineCode) {
        MemberStampView view = mock(MemberStampView.class);
        lenient().when(view.getStationId()).thenReturn(stationId);
        lenient().when(view.getStationName()).thenReturn(stationName);
        lenient().when(view.getLineId()).thenReturn(lineId);
        lenient().when(view.getLineName()).thenReturn(lineName);
        lenient().when(view.getLineCode()).thenReturn(lineCode);
        return view;
    }

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
    @DisplayName("존재하는 회원이면 역 목록을 스탬프 목록으로 반환한다")
    void getMemberStamps_success() {
        // given
        MemberStampView view = stampView(6L, "보문역", 6L, "6호선", LineCode.LINE_6);
        given(memberExistenceQueryService.existsMember(2L)).willReturn(true);
        given(memberStampRepository.findMemberStampsByMemberId(2L)).willReturn(List.of(view));

        // when
        MemberStampListResponse response = memberStampQueryService.getMemberStamps(2L);

        // then
        assertThat(response.stampCount()).isEqualTo(1);
        assertThat(response.stamps()).containsExactly(
                new MemberStampResponse(6L, "보문역", new LineSummaryResponse(6L, "6호선", LineCode.LINE_6)));
    }

    @Test
    @DisplayName("1호선 → 9호선 순으로 정렬하고, 대표 호선이 없는 역은 맨 뒤로 보낸다")
    void getMemberStamps_sortsByLineOrderWithNoLineLast() {
        // given: 일부러 뒤섞어서 넘긴다 (3호선, 노선없음, 1호선)
        MemberStampView line3 = stampView(3L, "역C", 3L, "3호선", LineCode.LINE_3);
        MemberStampView noLine = stampView(2L, "역B", null, null, null);
        MemberStampView line1 = stampView(1L, "역A", 1L, "1호선", LineCode.LINE_1);
        given(memberExistenceQueryService.existsMember(2L)).willReturn(true);
        given(memberStampRepository.findMemberStampsByMemberId(2L)).willReturn(List.of(line3, noLine, line1));

        // when
        MemberStampListResponse response = memberStampQueryService.getMemberStamps(2L);

        // then
        assertThat(response.stamps()).extracting(MemberStampResponse::stationId)
                .containsExactly(1L, 3L, 2L);
    }

    @Test
    @DisplayName("동일 호선 내에서는 역명 가나다순으로 정렬한다")
    void getMemberStamps_sortsByStationNameWithinSameLine() {
        // given: 같은 2호선인데 역명 순서를 뒤섞어서 넘긴다
        MemberStampView na = stampView(2L, "나역", 2L, "2호선", LineCode.LINE_2);
        MemberStampView da = stampView(3L, "다역", 2L, "2호선", LineCode.LINE_2);
        MemberStampView ga = stampView(1L, "가역", 2L, "2호선", LineCode.LINE_2);
        given(memberExistenceQueryService.existsMember(2L)).willReturn(true);
        given(memberStampRepository.findMemberStampsByMemberId(2L)).willReturn(List.of(na, da, ga));

        // when
        MemberStampListResponse response = memberStampQueryService.getMemberStamps(2L);

        // then
        assertThat(response.stamps()).extracting(MemberStampResponse::stationName)
                .containsExactly("가역", "나역", "다역");
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
