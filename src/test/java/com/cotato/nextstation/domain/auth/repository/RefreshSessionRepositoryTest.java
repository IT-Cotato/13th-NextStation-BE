package com.cotato.nextstation.domain.auth.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

// Lua 스크립트 자체의 동작(회전/grace/상한)은 실제 Redis가 있어야 검증되므로, 여기서는 반환값 해석만 다룬다.
@ExtendWith(MockitoExtension.class)
class RefreshSessionRepositoryTest {

    @InjectMocks
    private RefreshSessionRepository refreshSessionRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    private void givenIndexContains(Set<String> familyIds) {
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.members("auth:member-sessions:1")).willReturn(familyIds);
    }

    @SuppressWarnings("unchecked")
    private void givenRotateReturns(List<?> result) {
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .willReturn((List<Object>) result);
    }

    @Test
    @DisplayName("정상 반환이면 상태와 jti를 그대로 해석한다")
    void rotate_success() {
        // given
        givenRotateReturns(List.of("OK", "jti-2"));

        // when
        RefreshSessionRepository.RotateResult result =
                refreshSessionRepository.rotate("family-1", "jti-1", "jti-2", 1L);

        // then
        assertThat(result.status()).isEqualTo(RefreshSessionRepository.RotateStatus.OK);
        assertThat(result.jti()).isEqualTo("jti-2");
    }

    @Test
    @DisplayName("알 수 없는 상태 문자열이 오면 500 대신 NOT_FOUND로 떨어뜨린다")
    void rotate_unknownStatus() {
        // given - 스크립트와 enum이 어긋난 상황
        givenRotateReturns(List.of("SOMETHING_ELSE", ""));

        // when
        RefreshSessionRepository.RotateResult result =
                refreshSessionRepository.rotate("family-1", "jti-1", "jti-2", 1L);

        // then
        assertThat(result.status()).isEqualTo(RefreshSessionRepository.RotateStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("반환 원소 개수가 다르면 NOT_FOUND로 떨어뜨린다")
    void rotate_unexpectedSize() {
        // given
        givenRotateReturns(List.of("OK"));

        // when
        RefreshSessionRepository.RotateResult result =
                refreshSessionRepository.rotate("family-1", "jti-1", "jti-2", 1L);

        // then
        assertThat(result.status()).isEqualTo(RefreshSessionRepository.RotateStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("반환값이 null이어도 NOT_FOUND로 떨어뜨린다")
    void rotate_nullResult() {
        // given
        givenRotateReturns(null);

        // when
        RefreshSessionRepository.RotateResult result =
                refreshSessionRepository.rotate("family-1", "jti-1", "jti-2", 1L);

        // then
        assertThat(result.status()).isEqualTo(RefreshSessionRepository.RotateStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("회원의 모든 세션과 인덱스를 함께 삭제한다")
    void deleteAllOf_deletesEverySession() {
        // given
        givenIndexContains(new LinkedHashSet<>(List.of("family-1", "family-2")));

        // when
        int deleted = refreshSessionRepository.deleteAllOf(1L);

        // then
        assertThat(deleted).isEqualTo(2);
        then(redisTemplate).should()
                .delete(List.of("auth:refresh-session:family-1", "auth:refresh-session:family-2"));
        then(redisTemplate).should().delete("auth:member-sessions:1");
    }

    @Test
    @DisplayName("인덱스가 비어 있어도 실패하지 않는다 - 로그인한 적 없는 회원의 탈퇴")
    void deleteAllOf_emptyIndex() {
        // given
        givenIndexContains(Set.of());

        // when
        int deleted = refreshSessionRepository.deleteAllOf(1L);

        // then
        assertThat(deleted).isZero();
        then(redisTemplate).should(never()).delete(anyList());
        then(redisTemplate).should().delete("auth:member-sessions:1");
    }

    @Test
    @DisplayName("인덱스 조회가 null을 반환해도 실패하지 않는다")
    void deleteAllOf_nullIndex() {
        // given
        givenIndexContains(null);

        // when
        int deleted = refreshSessionRepository.deleteAllOf(1L);

        // then
        assertThat(deleted).isZero();
        then(redisTemplate).should().delete("auth:member-sessions:1");
    }
}