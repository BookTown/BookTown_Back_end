package hello.booktown.controller;

import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@Tag(name = "User API", description = "소셜 로그인 사용자 정보 및 탈퇴 API")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final RefreshTokenService refreshTokenService;

    public UserController(JwtTokenProvider jwtTokenProvider,
                          StringRedisTemplate redisTemplate,
                          RefreshTokenService refreshTokenService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.refreshTokenService = refreshTokenService;
    }

    @Operation(summary = "내 정보 확인", description = "현재 로그인한 사용자의 정보를 확인합니다.")
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal String username) {
        return ResponseEntity.ok().body("현재 로그인한 사용자: " + username);
    }

    @Operation(summary = "회원 탈퇴", description = "JWT 토큰을 통해 현재 로그인한 사용자를 삭제하고 로그아웃 처리합니다.")
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal String username, HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            long expiration = jwtTokenProvider.getExpiration(token);
            redisTemplate.opsForValue().set(token, "logout", expiration, TimeUnit.MILLISECONDS);
        }

        refreshTokenService.deleteRefreshToken(username);

        return ResponseEntity.ok("회원 탈퇴 및 로그아웃 완료");
    }
}