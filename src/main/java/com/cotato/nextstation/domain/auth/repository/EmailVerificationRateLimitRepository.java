package com.cotato.nextstation.domain.auth.repository;

import com.cotato.nextstation.domain.auth.entity.VerificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

// 인증번호 자체는 EmailVerification(JPA)에 저장하고, 여기선 발송 rate limit(시간당/일일 카운터, lock)만 Redis로 관리한다.
@Repository
@RequiredArgsConstructor
public class EmailVerificationRateLimitRepository {

    // 시간당 발송 횟수 카운터 키 (TTL 1시간)
    private static final String HOURLY_COUNT_KEY_FORMAT = "email:verify:count:hour:%s:%s";

    // 일일 발송 횟수 카운터 키 (TTL 1일)
    private static final String DAILY_COUNT_KEY_FORMAT = "email:verify:count:day:%s:%s";

    // 한도 초과 시 재요청 자체를 막는 잠금 키 (TTL = 잠금 기간)
    private static final String LOCK_KEY_FORMAT = "email:verify:lock:%s:%s";
    private static final String LOCK_VALUE = "LOCKED";

    private final RedisTemplate<String, String> redisTemplate;

    public long incrementHourlyCount(VerificationType type, String email) {
        return increment(hourlyCountKey(type, email), Duration.ofHours(1));
    }

    public long incrementDailyCount(VerificationType type, String email) {
        return increment(dailyCountKey(type, email), Duration.ofDays(1));
    }

    public boolean existsLock(VerificationType type, String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey(type, email)));
    }

    // 한도를 초과한 이메일을 lockDuration 동안 잠가 재요청을 차단한다.
    public void setLock(VerificationType type, String email, Duration lockDuration) {
        redisTemplate.opsForValue().set(lockKey(type, email), LOCK_VALUE, lockDuration);
    }

    // count == 1(새로 생성된 키)일 때만 EXPIRE를 걸어야 TTL 없는 키가 영구히 남는 걸 방지한다.
    private long increment(String key, Duration ttl) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return 0L;
        }
        if (count == 1L) {
            redisTemplate.expire(key, ttl);
        }
        return count;
    }

    private String hourlyCountKey(VerificationType type, String email) {
        return HOURLY_COUNT_KEY_FORMAT.formatted(type, email);
    }

    private String dailyCountKey(VerificationType type, String email) {
        return DAILY_COUNT_KEY_FORMAT.formatted(type, email);
    }

    private String lockKey(VerificationType type, String email) {
        return LOCK_KEY_FORMAT.formatted(type, email);
    }
}