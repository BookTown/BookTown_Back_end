package hello.booktown.controller;

import hello.booktown.domain.User;
import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "User API", description = "소셜 로그인, 사용자 정보, 로그아웃/회원탈퇴 API")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    public UserController(JwtTokenProvider jwtTokenProvider,
                          StringRedisTemplate redisTemplate,
                          UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
    }

    @Operation(
        summary = "내 정보 조회",
        description = "현재 로그인한 사용자의 정보를 조회합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "성공적으로 사용자 정보를 조회함"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
        }
    )
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal String userId) {
        Long id = Long.parseLong(userId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(user);
    }

    @Operation(
        summary = "회원 탈퇴",
        description = "JWT 토큰을 기반으로 현재 로그인한 사용자를 삭제하고 로그아웃 처리합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 인증 실패")
        }
    )

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal String userId, HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            long expiration = jwtTokenProvider.getExpiration(token);
            redisTemplate.opsForValue().set(token, "logout", expiration, TimeUnit.MILLISECONDS);
        }
        userRepository.deleteById(Long.parseLong(userId));
        return ResponseEntity.ok("회원 탈퇴 및 로그아웃 완료");
    }

    @Operation(
        summary = "로그아웃",
        description = "Redis에 AccessToken을 블랙리스트로 등록하여 로그아웃 처리합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "로그아웃 완료"),
            @ApiResponse(responseCode = "401", description = "토큰 인증 실패")
        }
    )
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            long expiration = jwtTokenProvider.getExpiration(token);
            if (expiration <= 0) {
                return ResponseEntity.status(401).body("이미 만료된 토큰입니다.");
            }
            redisTemplate.opsForValue().set(token, "logout", expiration, TimeUnit.MILLISECONDS);
        }
        return ResponseEntity.ok("로그아웃 완료");
    }

    @Operation(
        summary = "소셜 로그인 성공 콜백",
        description = "OAuth2 로그인 성공 후 사용자 등록 및 JWT 토큰을 발급합니다.\n\n" +
                     "AccessToken은 본문에 반환, RefreshToken은 HttpOnly 쿠키로 발급.",
        responses = {
            @ApiResponse(responseCode = "200", description = "토큰 발급 성공"),
            @ApiResponse(responseCode = "400", description = "OAuth2 필수 정보 누락")
        }
    )
    @GetMapping("/login/success")
    public ResponseEntity<Map<String, String>> loginSuccess(@AuthenticationPrincipal OAuth2User oAuth2User, HttpServletResponse response) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String accessToken = (String) attributes.get("accessToken");
        String refreshToken = (String) attributes.get("refreshToken");

        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) jwtTokenProvider.getRefreshExpirationTime() / 1000);
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of(
                "grantType", "Bearer",
                "accessToken", accessToken,
                "refreshToken", "httpOnly"
        ));
    }
}