package com.cotato.nextstation.domain.stamp.service.query;

import com.cotato.nextstation.domain.stamp.repository.MemberStampRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
}
