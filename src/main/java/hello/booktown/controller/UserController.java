package hello.booktown.controller;

import hello.booktown.domain.User;
import hello.booktown.dto.TokenDto;
import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.repository.UserRepository;
import hello.booktown.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Tag(name = "User API", description = "소셜 로그인, 사용자 정보, 로그아웃/회원탈퇴 API")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    public UserController(JwtTokenProvider jwtTokenProvider,
                          StringRedisTemplate redisTemplate,
                          RefreshTokenService refreshTokenService,
                          UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
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

    @Operation(summary = "로그아웃", description = "Redis에 토큰을 블랙리스트 등록하여 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            long expiration = jwtTokenProvider.getExpiration(token);
            redisTemplate.opsForValue().set(token, "logout", expiration, TimeUnit.MILLISECONDS); // 블랙리스트 등록
        }

        return ResponseEntity.ok("로그아웃 완료");
    }

    @Operation(summary = "소셜 로그인 성공 처리", description = "OAuth2 로그인 성공 후 사용자 등록 및 JWT 토큰 발급 처리.")
    @GetMapping("/login/success")
    public ResponseEntity<Map<String, String>> loginSuccess(@AuthenticationPrincipal OAuth2User oAuth2User, HttpServletResponse response) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String provider = (String) attributes.get("provider");
        String providerId = (String) attributes.get("providerId");
        String email = (String) attributes.get("email"); // nullable 허용
        String username = (String) attributes.get("username");
        String profileImage = (String) attributes.get("profileImage");

        if (provider == null || providerId == null) {
            throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("OAuth2 필수 정보 누락 (provider, providerId)");
        }

        String accessToken = (String) attributes.get("accessToken");
        String refreshToken = (String) attributes.get("refreshToken");

        return ResponseEntity.ok(Map.of(
                "grantType", "Bearer",
                "accessToken", accessToken,
                "refreshToken", "httpOnly"
        ));
    }


}