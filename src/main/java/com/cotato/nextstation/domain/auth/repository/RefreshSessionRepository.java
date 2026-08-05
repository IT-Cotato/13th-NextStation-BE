package com.cotato.nextstation.domain.auth.repository;

import com.cotato.nextstation.global.jwt.AuthTokenClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Set;

// refreshToken rotation의 세션 상태(familyId -> 현재 유효한 jti)를 Redis Hash로 관리한다.
// 조회/비교/교체를 Lua로 원자화해 동시 요청이 서로의 갱신을 덮어쓰지 않게 한다.
@Slf4j
@Repository
@RequiredArgsConstructor
public class RefreshSessionRepository {

    private static final String SESSION_KEY_FORMAT = "auth:refresh-session:%s";

    // 세션은 familyId(기기) 단위로만 저장돼 "이 회원의 모든 세션"을 찾을 수 없다.
    // 탈퇴처럼 회원 단위로 세션을 전부 끊어야 할 때를 위해 memberId -> familyId 목록 인덱스를 따로 둔다.
    private static final String MEMBER_SESSIONS_KEY_FORMAT = "auth:member-sessions:%d";

    // rotate할 때마다 TTL을 다시 채우는 sliding 방식
    // 활발히 쓰는 유저가 14일마다 강제 로그아웃되는 걸 막되, 세션이 무한정 살아있지 않도록 최초 로그인 기준 절대 상한을 함께 둔다.
    private static final Duration SLIDING_EXPIRATION = AuthTokenClaims.REFRESH_TOKEN_EXPIRATION;
    private static final Duration ABSOLUTE_EXPIRATION = Duration.ofDays(90);

    // rotate 직후 이 시간 동안은 직전 jti로 온 요청도 정상으로 본다.
    // 멀티탭이나 병렬 API 호출이 같은 refreshToken으로 동시에 reissue를 하는 경우를 탈취로 오탐하지 않기 위한 창
    private static final Duration REUSE_GRACE = Duration.ofSeconds(10);

    // HSET과 PEXPIRE 사이에 프로세스가 죽으면 TTL 없는 세션이 영구히 남으므로 한 번에 처리한다.
    private static final RedisScript<Boolean> CREATE_SCRIPT = new DefaultRedisScript<>("""
            redis.call('HSET', KEYS[1], 'memberId', ARGV[1], 'jti', ARGV[2], 'absoluteExpiry', ARGV[3])
            redis.call('PEXPIRE', KEYS[1], ARGV[4])
            return 1
            """, Boolean.class);

    // CREATE_SCRIPT와 같은 이유로 원자화한다. SADD 직후 죽으면 TTL 없는 인덱스가 영구히 남는다.
    private static final RedisScript<Boolean> ADD_TO_INDEX_SCRIPT = new DefaultRedisScript<>("""
            redis.call('SADD', KEYS[1], ARGV[1])
            redis.call('PEXPIRE', KEYS[1], ARGV[2])
            return 1
            """, Boolean.class);

    // 반환: {상태, 이 요청이 사용해야 할 jti}
    private static final RedisScript<List> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            local currentJti = redis.call('HGET', KEYS[1], 'jti')
            if not currentJti then
                return {'NOT_FOUND', ''}
            end

            local now = tonumber(ARGV[3])
            -- 필드가 없으면 HGET이 false를 반환해 비교에서 런타임 오류(=500)가 난다.
            -- or 0으로 떨어뜨려 손상된 세션을 만료로 간주하고 정리한다.
            local absoluteExpiry = tonumber(redis.call('HGET', KEYS[1], 'absoluteExpiry') or 0)
            if now >= absoluteExpiry then
                redis.call('DEL', KEYS[1])
                return {'NOT_FOUND', ''}
            end

            if redis.call('HGET', KEYS[1], 'memberId') ~= ARGV[6] then
                redis.call('DEL', KEYS[1])
                return {'MEMBER_MISMATCH', ''}
            end

            -- sliding: 남은 절대 수명을 넘지 않는 선에서 TTL을 다시 채운다
            local ttl = tonumber(ARGV[5])
            if absoluteExpiry - now < ttl then
                ttl = absoluteExpiry - now
            end

            if currentJti ~= ARGV[1] then
                -- 이미 rotate된 직전 jti가 grace 안에 다시 왔다면 동시 요청으로 보고, rotate 없이 현재 토큰을 그대로 돌려준다.
                -- (여기서 또 rotate하면 같은 jti를 든 세 번째 요청부터 다시 어긋난다)
                local previousJti = redis.call('HGET', KEYS[1], 'previousJti')
                local previousJtiUntil = tonumber(redis.call('HGET', KEYS[1], 'previousJtiUntil') or 0)
                if previousJti == ARGV[1] and now < previousJtiUntil then
                    redis.call('PEXPIRE', KEYS[1], ttl)
                    return {'GRACE', currentJti}
                end
                redis.call('DEL', KEYS[1])
                return {'REUSE_DETECTED', ''}
            end

            redis.call('HSET', KEYS[1], 'jti', ARGV[2], 'previousJti', currentJti, 'previousJtiUntil', now + tonumber(ARGV[4]))
            redis.call('PEXPIRE', KEYS[1], ttl)
            return {'OK', ARGV[2]}
            """, List.class);

    private final RedisTemplate<String, String> redisTemplate;

    public void create(String familyId, Long memberId, String jti) {
        long absoluteExpiry = System.currentTimeMillis() + ABSOLUTE_EXPIRATION.toMillis();
        redisTemplate.execute(
                CREATE_SCRIPT,
                List.of(sessionKey(familyId)),
                memberId.toString(),
                jti,
                String.valueOf(absoluteExpiry),
                String.valueOf(SLIDING_EXPIRATION.toMillis())
        );
    }

    /**
     * currentJti와 일치할 때만 newJti로 원자적으로 교체하고 TTL을 갱신한다.
     * grace 안의 직전 jti면 교체 없이 현재 jti를 그대로 돌려주고, 그 밖의 불일치는 탈취로 보고 세션을 삭제한다.
     */
    public RotateResult rotate(String familyId, String currentJti, String newJti, Long memberId) {
        List<?> result = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(sessionKey(familyId)),
                currentJti,
                newJti,
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(REUSE_GRACE.toMillis()),
                String.valueOf(SLIDING_EXPIRATION.toMillis()),
                memberId.toString()
        );

        return toRotateResult(familyId, result);
    }

    /**
     * 스크립트 반환값이 예상과 다르면 세션이 없는 것으로 보고 재로그인을 유도한다.
     * 그대로 valueOf를 태우면 IllegalArgumentException이 500으로 나가고, 그 세션은 TTL 만료까지 복구되지 않는다.
     * 정상 흐름에서는 발생할 수 없으므로 원인 추적을 위해 error로 남긴다.
     */
    private RotateResult toRotateResult(String familyId, List<?> result) {
        if (result == null || result.size() != 2) {
            log.error("rotate 스크립트 반환 형태가 예상과 다름: familyId={}, result={}", familyId, result);
            return new RotateResult(RotateStatus.NOT_FOUND, null);
        }

        String status = String.valueOf(result.get(0));
        Object jti = result.get(1);
        try {
            return new RotateResult(RotateStatus.valueOf(status), jti == null ? null : jti.toString());
        } catch (IllegalArgumentException e) {
            log.error("rotate 스크립트가 알 수 없는 상태를 반환(스크립트-enum 불일치): familyId={}, status={}", familyId, status);
            return new RotateResult(RotateStatus.NOT_FOUND, null);
        }
    }

    public void delete(String familyId) {
        redisTemplate.delete(sessionKey(familyId));
    }

    /**
     * 회원의 세션 인덱스에 familyId를 추가한다. TTL은 SADD마다 세션 절대 상한과 같은 값으로 다시 채운다.
     * rotate는 familyId를 바꾸지 않으므로 인덱스를 갱신할 필요가 없다.
     */
    public void addToMemberIndex(Long memberId, String familyId) {
        redisTemplate.execute(
                ADD_TO_INDEX_SCRIPT,
                List.of(memberSessionsKey(memberId)),
                familyId,
                String.valueOf(ABSOLUTE_EXPIRATION.toMillis())
        );
    }

    public void removeFromMemberIndex(Long memberId, String familyId) {
        redisTemplate.opsForSet().remove(memberSessionsKey(memberId), familyId);
    }

    /**
     * 회원의 모든 기기 세션을 삭제한다. 삭제된 세션 수를 반환한다.
     * <p>
     * 인덱스에는 이미 사라진 familyId가 남아있을 수 있다(TTL 만료, 재사용 탐지로 인한 세션 강제 삭제 등).
     * DEL은 없는 키에도 안전하고 인덱스 자체가 TTL로 정리되므로 그대로 둔다.
     * 조회와 삭제를 원자화하지 않는 이유는, 이 메서드가 탈퇴 커밋 이후에 호출돼 그 사이 새 세션이 생길 수 없기 때문이다
     * (로그인은 status != ACTIVE에서 막힌다).
     */
    public int deleteAllOf(Long memberId) {
        String indexKey = memberSessionsKey(memberId);
        Set<String> familyIds = redisTemplate.opsForSet().members(indexKey);

        if (familyIds != null && !familyIds.isEmpty()) {
            redisTemplate.delete(familyIds.stream().map(this::sessionKey).toList());
        }
        redisTemplate.delete(indexKey);

        return familyIds == null ? 0 : familyIds.size();
    }

    private String sessionKey(String familyId) {
        return SESSION_KEY_FORMAT.formatted(familyId);
    }

    private String memberSessionsKey(Long memberId) {
        return MEMBER_SESSIONS_KEY_FORMAT.formatted(memberId);
    }

    public record RotateResult(RotateStatus status, String jti) {
    }

    public enum RotateStatus {
        OK,
        GRACE,
        REUSE_DETECTED,
        MEMBER_MISMATCH,
        NOT_FOUND
    }
}