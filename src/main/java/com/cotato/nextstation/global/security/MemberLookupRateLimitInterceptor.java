package com.cotato.nextstation.global.security;

import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import com.cotato.nextstation.global.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * 회원 조회 API(GET /api/v1/members/**)를 "로그인한 요청자" 기준으로 분당 호출 횟수를 제한한다.
 * memberId를 1부터 순회하며 닉네임·프로필 이미지·스탬프를 긁어가는 전수 스캔을 막기 위함이다.
 * IP 대신 요청자 memberId를 키로 쓰는 이유: 이 API들은 이미 accessToken이 필수라 "누가"
 * 요청했는지 알고 있고, 같은 계정이 짧은 시간에 여러 memberId를 순회하는 패턴이 IP보다
 * 훨씬 정확한 스캔 신호이기 때문이다.
 * <p>
 * 인증 검증 자체는 컨트롤러 진입 시점에 JwtPrincipalArgumentResolver가 담당한다.
 * 여기서는 토큰이 없거나 파싱에 실패하면 그냥 통과시킨다 - 401 처리는 그쪽 책임이고,
 * 이 인터셉터가 인증 실패를 대신 판단하면 두 곳의 에러 처리가 어긋날 수 있다.
 * <p>
 * 주의: 아직 WebConfig.addInterceptors()에 등록돼 있지 않아 실제로는 요청 경로를 타지 않는다
 * (프로덕션에서 레이트리밋 미적용). 예전엔 @Component였는데, 그 상태로는 Spring Boot의
 * @WebMvcTest가 HandlerInterceptor 구현체를 슬라이스에 자동 포함시키면서 등록되지도 않은 이
 * 빈 때문에 RedisTemplate<String,String> 빈이 없어 모든 @WebMvcTest가 컨텍스트 로딩에서
 * 깨졌다. 실제로 등록해서 쓰려면 @Component를 복원하고 addInterceptors()에 추가한 뒤,
 * 그 슬라이스를 쓰는 모든 @WebMvcTest에 RedisTemplate mock을 같이 추가해야 한다.
 */
@Slf4j
@RequiredArgsConstructor
public class MemberLookupRateLimitInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REDIS_KEY_PREFIX = "ratelimit:member-lookup:";

    // TODO: 임시로 잡은 값. 실제 트래픽 패턴 확인 후 조정 필요.
    private static final long LIMIT_PER_WINDOW = 60;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long requesterId = resolveMemberId(request);
        if (requesterId == null) {
            return true;
        }

        String key = REDIS_KEY_PREFIX + requesterId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, WINDOW);
        }
        if (count != null && count > LIMIT_PER_WINDOW) {
            log.warn("회원 조회 API 호출 한도 초과: requesterId={}, count={}", requesterId, count);
            throw new CustomException(GlobalErrorCode.TOO_MANY_REQUESTS);
        }

        return true;
    }

    // 여기서 실패하는 모든 경우(헤더 없음/형식 오류/서명 위조/만료 등)는 인증 실패로,
    // 카운팅 없이 그냥 통과시켜 컨트롤러 진입 시점의 401 처리에 맡긴다.
    private Long resolveMemberId(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return null;
        }

        try {
            Claims claims = jwtProvider.parseClaims(token);
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | NumberFormatException e) {
            return null;
        }
    }
}
