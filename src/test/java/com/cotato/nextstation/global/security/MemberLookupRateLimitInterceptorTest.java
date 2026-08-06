package com.cotato.nextstation.global.security;

import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import com.cotato.nextstation.global.jwt.JwtProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberLookupRateLimitInterceptorTest {

    @InjectMocks
    private MemberLookupRateLimitInterceptor interceptor;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private static final String TOKEN = "access-token";
    private static final String REDIS_KEY = "ratelimit:member-lookup:1";

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        return request;
    }

    private void stubValidToken() {
        given(jwtProvider.parseClaims(TOKEN)).willReturn(Jwts.claims().subject("1").build());
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 카운팅하지 않고 통과시킨다")
    void preHandle_missingAuthorizationHeader_skipsCounting() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        // when
        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        // then: 401 처리는 컨트롤러 진입 시점의 리졸버 책임이라 여기서는 그냥 통과한다
        assertThat(result).isTrue();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("위변조된 토큰이면 카운팅하지 않고 통과시킨다")
    void preHandle_invalidToken_skipsCounting() {
        // given
        given(jwtProvider.parseClaims(TOKEN)).willThrow(new MalformedJwtException("malformed"));
        MockHttpServletRequest request = requestWithToken(TOKEN);

        // when
        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        // then
        assertThat(result).isTrue();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("한도 이내면 통과시키고 첫 요청이면 만료 시간을 설정한다")
    void preHandle_firstRequestWithinLimit_setsExpire() {
        // given
        stubValidToken();
        given(valueOperations.increment(REDIS_KEY)).willReturn(1L);
        MockHttpServletRequest request = requestWithToken(TOKEN);

        // when
        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        // then
        assertThat(result).isTrue();
        verify(redisTemplate).expire(REDIS_KEY, Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("한도 이내의 두 번째 이후 요청은 만료 시간을 다시 설정하지 않는다")
    void preHandle_subsequentRequestWithinLimit_doesNotResetExpire() {
        // given
        stubValidToken();
        given(valueOperations.increment(REDIS_KEY)).willReturn(2L);
        MockHttpServletRequest request = requestWithToken(TOKEN);

        // when
        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        // then
        assertThat(result).isTrue();
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("한도를 초과하면 예외가 발생한다")
    void preHandle_exceedsLimit_throwsTooManyRequests() {
        // given
        stubValidToken();
        given(valueOperations.increment(REDIS_KEY)).willReturn(61L);
        MockHttpServletRequest request = requestWithToken(TOKEN);

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(GlobalErrorCode.TOO_MANY_REQUESTS.getMessage());
    }
}
