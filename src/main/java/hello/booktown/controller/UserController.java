package hello.booktown.controller;

import hello.booktown.dto.UserLoginRequest;
import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.service.RefreshTokenService;
import hello.booktown.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "User API", description = "회원가입 / 로그인 / 회원정보 관련 API")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final RefreshTokenService refreshTokenService;

    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider,
                          StringRedisTemplate redisTemplate,
                          RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.refreshTokenService = refreshTokenService;
    }

    @Operation(summary = "회원가입", description = "사용자 정보를 받아 회원가입을 수행합니다.")
    @ApiResponse(responseCode = "200", description = "회원가입 성공")
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserLoginRequest request) {
        userService.register(request.getUsername(), request.getPassword());
        return ResponseEntity.ok("회원가입 완료");
    }

    @Operation(summary = "로그인", description = "사용자의 아이디/비밀번호를 받아 JWT 토큰을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 오류")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest request, HttpServletResponse response) {
        return userService.login(request.getUsername(), request.getPassword())
                .map(user -> {
                    String accessToken = jwtTokenProvider.generateToken(user.getUsername());
                    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

                    refreshTokenService.saveRefreshToken(
                            user.getUsername(),
                            refreshToken,
                            jwtTokenProvider.getRefreshTokenRemainingMillis(refreshToken)
                    );

                    // HttpOnly 쿠키 설정
                    Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
                    refreshCookie.setHttpOnly(true);
                    refreshCookie.setPath("/");
                    refreshCookie.setMaxAge(7 * 24 * 60 * 60);
                    response.addCookie(refreshCookie);

                    return ResponseEntity.ok(Map.of(
                            "grantType", "Bearer",
                            "accessToken", accessToken,
                            "refreshToken", "httpOnly"
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "아이디 또는 비밀번호가 틀렸습니다.")));
    }

    @Operation(summary = "내 정보 확인", description = "현재 로그인한 사용자의 정보를 확인합니다.")
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal String username) {
        return ResponseEntity.ok().body("현재 로그인한 사용자: " + username);
    }

    @Operation(summary = "회원 탈퇴", description = "JWT 토큰을 통해 현재 로그인한 사용자를 삭제하고 로그아웃 처리합니다.")
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal String username, HttpServletRequest request) {
        userService.deleteByUsername(username);

        String token = jwtTokenProvider.resolveToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            long expiration = jwtTokenProvider.getExpiration(token);
            redisTemplate.opsForValue().set(token, "logout", expiration, TimeUnit.MILLISECONDS);
        }

        refreshTokenService.deleteRefreshToken(username);

        return ResponseEntity.ok("회원 탈퇴 및 로그아웃 완료");
    }
}