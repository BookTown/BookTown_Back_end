package hello.booktown.controller;

import hello.booktown.domain.User;
import hello.booktown.domain.enums.Difficulty;
import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.repository.UserRepository;
import hello.booktown.util.S3Uploader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "User API", description = "소셜 로그인, 사용자 정보, 로그아웃/회원탈퇴 API")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    private final S3Uploader s3Uploader;

    public UserController(JwtTokenProvider jwtTokenProvider,
                          StringRedisTemplate redisTemplate,
                          UserRepository userRepository,
                          S3Uploader s3Uploader) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.s3Uploader = s3Uploader;
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
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal String userId, HttpServletRequest request, HttpServletResponse response) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            long expiration = jwtTokenProvider.getExpiration(token);
            redisTemplate.opsForValue().set(token, "logout", expiration, TimeUnit.MILLISECONDS);
        }

        redisTemplate.delete("RT:" + userId);

        Cookie expiredCookie = new Cookie("refreshToken", null);
        expiredCookie.setMaxAge(0);
        expiredCookie.setPath("/");
        expiredCookie.setHttpOnly(true);
        response.addCookie(expiredCookie);

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
    public ResponseEntity<?> logout(@AuthenticationPrincipal String userId, HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
        }

        String tokenUserId = jwtTokenProvider.getUsernameFromToken(token);
        if (!tokenUserId.equals(userId)) {
            return ResponseEntity.status(401).body("토큰 사용자 정보가 일치하지 않습니다.");
        }

        long expiration = jwtTokenProvider.getExpiration(token);
        if (expiration <= 0) {
            return ResponseEntity.status(401).body("이미 만료된 토큰입니다.");
        }

        redisTemplate.opsForValue().set(token, "logout", expiration, TimeUnit.MILLISECONDS);
        return ResponseEntity.ok("로그아웃 완료");
    }

}