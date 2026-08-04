package com.cotato.nextstation.domain.stamp.service.query;

import com.cotato.nextstation.domain.stamp.converter.MemberStampConverter;
import com.cotato.nextstation.domain.stamp.dto.response.MyStampDetailResponse;
import com.cotato.nextstation.domain.stamp.dto.response.MyStampListResponse;
import com.cotato.nextstation.domain.stamp.exception.StampErrorCode;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository.MyStampDetailView;
import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository.MyStampView;
import com.cotato.nextstation.domain.station.entity.LineCode;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
    private MemberStampConverter memberStampConverter;

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
    @DisplayName("1호선 → 9호선 순으로 정렬하고, 대표 호선이 없는 역은 맨 뒤로 보낸다")
    void getMyStamps_sortsByLineOrder_nullLineLast() {
        // given: 리포지토리 응답 순서와 무관하게 노선 순으로 재정렬돼야 한다
        MyStampView line6 = stampView(1L, LineCode.LINE_6);
        MyStampView noLine = stampView(2L, null);
        MyStampView line1 = stampView(3L, LineCode.LINE_1);

        given(memberStampRepository.findMyStampsByMemberId(1L))
                .willReturn(List.of(line6, noLine, line1));
        given(memberStampConverter.toMyStampListResponse(any())).willReturn(mock(MyStampListResponse.class));

        // when
        memberStampQueryService.getMyStamps(1L);

        // then
        ArgumentCaptor<List<MyStampView>> captor = ArgumentCaptor.forClass(List.class);
        verify(memberStampConverter).toMyStampListResponse(captor.capture());

        assertThat(captor.getValue())
                .extracting(MyStampView::getStationId)
                .containsExactly(3L, 1L, 2L);
    }

    @Test
    @DisplayName("컨버터가 만든 응답을 그대로 반환한다")
    void getMyStamps_returnsConverterResponse() {
        // given
        given(memberStampRepository.findMyStampsByMemberId(1L)).willReturn(List.of());
        MyStampListResponse expected = mock(MyStampListResponse.class);
        given(memberStampConverter.toMyStampListResponse(any())).willReturn(expected);

        // when
        MyStampListResponse response = memberStampQueryService.getMyStamps(1L);

        // then
        assertThat(response).isSameAs(expected);
    }

    @Test
    @DisplayName("해당 역의 최초 방문 스탬프 상세와, 이 역에서 가장 이른 여행일지 id를 컨버터로 변환해 반환한다")
    void getMyStampDetail_success() {
        // given
        MyStampDetailView view = mock(MyStampDetailView.class);
        given(memberStampRepository.findEarliestStampByMemberIdAndStationId(eq(1L), eq(5L), any(Pageable.class)))
                .willReturn(List.of(view));
        given(memberStampRepository.findEarliestJournalIdByMemberIdAndStationId(eq(1L), eq(5L), any(Pageable.class)))
                .willReturn(List.of(42L));
        MyStampDetailResponse expected = mock(MyStampDetailResponse.class);
        given(memberStampConverter.toMyStampDetailResponse(view, 42L)).willReturn(expected);

        // when
        MyStampDetailResponse response = memberStampQueryService.getMyStampDetail(1L, 5L);

        // then
        assertThat(response).isSameAs(expected);
    }

    @Test
    @DisplayName("최초 완주 건에는 일지가 없어도, 이후 재완주 때 쓴 일지가 있으면 그 journalId를 사용한다")
    void getMyStampDetail_usesJournalFromLaterCompletionWhenFirstHasNone() {
        // given
        MyStampDetailView view = mock(MyStampDetailView.class);
        given(memberStampRepository.findEarliestStampByMemberIdAndStationId(eq(1L), eq(5L), any(Pageable.class)))
                .willReturn(List.of(view));
        given(memberStampRepository.findEarliestJournalIdByMemberIdAndStationId(eq(1L), eq(5L), any(Pageable.class)))
                .willReturn(List.of(99L));
        MyStampDetailResponse expected = mock(MyStampDetailResponse.class);
        given(memberStampConverter.toMyStampDetailResponse(view, 99L)).willReturn(expected);

        // when
        MyStampDetailResponse response = memberStampQueryService.getMyStampDetail(1L, 5L);

        // then
        assertThat(response).isSameAs(expected);
    }

    @Test
    @DisplayName("이 역에서 작성된 여행일지가 하나도 없으면 journalId는 null로 전달된다")
    void getMyStampDetail_nullJournalIdWhenNoJournalExists() {
        // given
        MyStampDetailView view = mock(MyStampDetailView.class);
        given(memberStampRepository.findEarliestStampByMemberIdAndStationId(eq(1L), eq(5L), any(Pageable.class)))
                .willReturn(List.of(view));
        given(memberStampRepository.findEarliestJournalIdByMemberIdAndStationId(eq(1L), eq(5L), any(Pageable.class)))
                .willReturn(List.of());
        MyStampDetailResponse expected = mock(MyStampDetailResponse.class);
        given(memberStampConverter.toMyStampDetailResponse(view, null)).willReturn(expected);

        // when
        MyStampDetailResponse response = memberStampQueryService.getMyStampDetail(1L, 5L);

        // then
        assertThat(response).isSameAs(expected);
    }

    @Test
    @DisplayName("해당 역에 방문 기록이 없으면 MEMBER_STAMP_NOT_FOUND 예외가 발생한다")
    void getMyStampDetail_notFound() {
        // given
        given(memberStampRepository.findEarliestStampByMemberIdAndStationId(eq(1L), eq(5L), any(Pageable.class)))
                .willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> memberStampQueryService.getMyStampDetail(1L, 5L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(StampErrorCode.MEMBER_STAMP_NOT_FOUND.getMessage());
    }

    // stationName은 getMyStamps의 정렬 로직이 참조하지 않아 스텁하지 않는다(불필요 스텁 경고 방지).
    private MyStampView stampView(Long stationId, LineCode lineCode) {
        MyStampView view = mock(MyStampView.class);
        given(view.getStationId()).willReturn(stationId);
        given(view.getLineCode()).willReturn(lineCode);
        return view;
    }
}
