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
import org.springframework.security.oauth2.core.user.OAuth2User;
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

    @PatchMapping("/introduction")
    @Operation(summary = "자기소개 수정", description = "사용자의 자기소개를 수정합니다.")
    public ResponseEntity<?> updateIntroduction(@AuthenticationPrincipal String userId,
                                                @RequestBody Map<String, String> body) {
        String newIntro = body.get("introduction");
        if (newIntro == null || newIntro.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("자기소개는 비워둘 수 없습니다.");
        }

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.updateIntroduction(newIntro);
        userRepository.save(user);

        return ResponseEntity.ok("자기소개가 수정되었습니다.");
    }

    @PatchMapping("/username")
    @Operation(summary = "이름 수정", description = "사용자의 이름을 수정합니다.")
    public ResponseEntity<?> updateUsername(@AuthenticationPrincipal String userId,
                                            @RequestBody Map<String, String> body) {
        String newUsername = body.get("username");
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("이름은 비워둘 수 없습니다.");
        }

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.updateUsername(newUsername);
        userRepository.save(user);

        return ResponseEntity.ok("이름이 수정되었습니다.");
    }

    @PatchMapping("/score")
    @Operation(summary = "점수 수정", description = "사용자의 점수를 수정합니다.")
    public ResponseEntity<?> updateScore(@AuthenticationPrincipal String userId,
                                         @RequestBody Map<String, Long> body) {
        Long newScore = body.get("score");
        if (newScore == null || newScore < 0) {
            return ResponseEntity.badRequest().body("점수는 0 이상이어야 합니다.");
        }

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.updateScore(newScore);
        userRepository.save(user);

        return ResponseEntity.ok("점수가 수정되었습니다.");
    }

    @PatchMapping("/difficulty")
    @Operation(summary = "퀴즈 난이도 수정", description = "사용자의 퀴즈 생성 기본 난이도를 수정합니다.")
    public ResponseEntity<?> updateDifficulty(@AuthenticationPrincipal String userId,
                                              @RequestBody Map<String, String> body) {
        String difficultyStr = body.get("difficulty");
        if (difficultyStr == null) {
            return ResponseEntity.badRequest().body("난이도를 입력해주세요.");
        }

        Difficulty difficulty;
        try {
            difficulty = Difficulty.valueOf(difficultyStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("잘못된 난이도 값입니다. (easy, medium, hard 중 하나여야 합니다.)");
        }

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.updateDifficulty(difficulty);
        userRepository.save(user);

        return ResponseEntity.ok("난이도가 수정되었습니다.");
    }


    @Operation(summary = "프로필 이미지 수정", description = "사용자의 프로필 이미지를 수정합니다.")
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfileImage(HttpServletRequest request,
                                                @RequestPart("file") MultipartFile file) {
        String token = jwtTokenProvider.resolveToken(request);

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
        }

        String userId = jwtTokenProvider.getUsernameFromToken(token);

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("파일이 비어있습니다.");
        }

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        String oldImageUrl = user.getProfileImage();
        if (oldImageUrl != null && oldImageUrl.contains("profile/")) {
            s3Uploader.delete(oldImageUrl);
        }
        String imageUrl = s3Uploader.upload(file);
        user.setProfileImage(imageUrl);
        userRepository.save(user);

        return ResponseEntity.ok("프로필 이미지가 수정되었습니다.");
    }

}