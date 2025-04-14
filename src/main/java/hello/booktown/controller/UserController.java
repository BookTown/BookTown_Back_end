package hello.booktown.controller;

import hello.booktown.domain.User;
import hello.booktown.exception.CustomException;
import hello.booktown.exception.ErrorCode;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal String userId) {
        Long id = Long.parseLong(userId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자를 삭제하고 로그아웃 처리합니다.")
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal String userId,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        String token = jwtTokenProvider.resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            long expiration = jwtTokenProvider.getExpiration(token);
            redisTemplate.opsForValue().set(token, "logout", expiration, TimeUnit.MILLISECONDS);
        } else {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
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

    @Operation(summary = "로그아웃", description = "AccessToken을 블랙리스트로 등록하여 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal String userId, HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String tokenUserId = jwtTokenProvider.getUsernameFromToken(token);
        if (!tokenUserId.equals(userId)) {
            throw new CustomException(ErrorCode.TOKEN_MISMATCH);
        }

        long expiration = jwtTokenProvider.getExpiration(token);
        if (expiration <= 0) {
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        }

        redisTemplate.opsForValue().set(token, "logout", expiration, TimeUnit.MILLISECONDS);
        return ResponseEntity.ok("로그아웃 완료");
    }
}