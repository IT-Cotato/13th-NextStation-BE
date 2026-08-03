package com.cotato.nextstation.domain.member.service;

import com.cotato.nextstation.domain.member.entity.Gender;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.member.repository.MemberSocialAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MemberPurgeBatchTest {

    @InjectMocks
    private MemberPurgeBatch memberPurgeBatch;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberSocialAccountRepository memberSocialAccountRepository;

    private Member withdrawnMember(Long id) {
        Member member = Member.builder().email("user@example.com").password("encoded").build();
        ReflectionTestUtils.setField(member, "id", id);
        member.completeProfile("환승러", "https://cdn.example.com/profile/1.png", Gender.MALE, LocalDate.of(2000, 1, 1));
        member.withdraw();
        ReflectionTestUtils.setField(member, "deletedAt", LocalDateTime.now().minusDays(8));
        return member;
    }

    @Test
    @DisplayName("유예가 지난 탈퇴 회원의 개인정보를 비우고 소셜 계정을 삭제한다")
    void purge_clearsPersonalData() {
        // given
        Member member = withdrawnMember(1L);
        given(memberRepository.findAllByStatusAndDeletedAtBeforeAndPurgedAtIsNull(eq(MemberStatus.WITHDRAWN), any()))
                .willReturn(List.of(member));

        // when
        memberPurgeBatch.purgeExpiredWithdrawals();

        // then
        assertThat(member.getEmail()).isNull();
        assertThat(member.getPassword()).isNull();
        assertThat(member.getProfileImageUrl()).isNull();
        assertThat(member.getBirthDate()).isNull();
        assertThat(member.getPurgedAt()).isNotNull();
        then(memberSocialAccountRepository).should().deleteByMemberId(1L);
    }

    @Test
    @DisplayName("닉네임과 탈퇴 상태는 파기 후에도 유지한다 - 콘텐츠 작성자 표시에 쓰인다")
    void purge_keepsNicknameAndStatus() {
        // given
        Member member = withdrawnMember(1L);
        given(memberRepository.findAllByStatusAndDeletedAtBeforeAndPurgedAtIsNull(eq(MemberStatus.WITHDRAWN), any()))
                .willReturn(List.of(member));

        // when
        memberPurgeBatch.purgeExpiredWithdrawals();

        // then
        assertThat(member.getNickname()).isEqualTo("환승러");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("대상이 없으면 아무것도 하지 않는다")
    void purge_noTargets() {
        // given
        given(memberRepository.findAllByStatusAndDeletedAtBeforeAndPurgedAtIsNull(eq(MemberStatus.WITHDRAWN), any()))
                .willReturn(List.of());

        // when
        memberPurgeBatch.purgeExpiredWithdrawals();

        // then
        then(memberSocialAccountRepository).should(never()).deleteByMemberId(any());
    }
}