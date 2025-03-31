package hello.booktown.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 리프레시 토큰 저장 (만료 시간 설정 필수)
    public void saveRefreshToken(String username, String refreshToken, long expirationMillis) {
        redisTemplate.opsForValue().set("refresh:" + username, refreshToken, expirationMillis, TimeUnit.MILLISECONDS);
    }

    // 리프레시 토큰 조회
    public Optional<String> getRefreshToken(String username) {
        String token = redisTemplate.opsForValue().get("refresh:" + username);
        return Optional.ofNullable(token);
    }

    // 리프레시 토큰 삭제
    public void deleteRefreshToken(String username) {
        redisTemplate.delete("refresh:" + username);
    }
}