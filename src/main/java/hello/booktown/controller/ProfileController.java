package hello.booktown.controller;

import hello.booktown.domain.User;
import hello.booktown.dto.ProfileUpdateRequestDto;
import hello.booktown.exception.CustomException;
import hello.booktown.exception.ErrorCode;
import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.repository.UserRepository;
import hello.booktown.util.S3Uploader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;
    private final JwtTokenProvider jwtTokenProvider;

    public ProfileController(UserRepository userRepository, S3Uploader s3Uploader, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.s3Uploader = s3Uploader;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Operation(
            summary = "사용자 프로필 조회",
            description = "지정된 사용자 ID를 통해 해당 사용자의 프로필 정보를 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "프로필 정보 조회 성공"),
                    @ApiResponse(responseCode = "404", description = "해당 ID의 사용자를 찾을 수 없음")
            }
    )
    @GetMapping("/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "사용자 프로필 정보 수정",
            description = "사용자가 자신의 이름, 자기소개, 점수, 난이도 정보를 선택적으로 수정합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "프로필 정보 수정 성공"),
                    @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰"),
                    @ApiResponse(responseCode = "403", description = "자신의 정보만 수정 가능"),
                    @ApiResponse(responseCode = "404", description = "사용자 정보 없음")
            }
    )
    @PatchMapping("/update/{userId}")
    public ResponseEntity<?> updateProfileInfo(@PathVariable Long userId,
                                               @RequestBody ProfileUpdateRequestDto dto,
                                               HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        String tokenUserId = jwtTokenProvider.getUsernameFromToken(token);
        if (!tokenUserId.equals(String.valueOf(userId))) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (dto.getUsername() != null) user.updateUsername(dto.getUsername());
        if (dto.getIntroduction() != null) user.updateIntroduction(dto.getIntroduction());
        if (dto.getScore() != null && dto.getScore() >= 0) user.updateScore(dto.getScore());

        userRepository.save(user);
        return ResponseEntity.ok("프로필 정보가 수정되었습니다.");
    }

    @Operation(
            summary = "프로필 이미지 수정",
            description = "로그인한 사용자가 자신의 프로필 이미지를 수정합니다. 이전 이미지가 존재할 경우 S3에서 삭제됩니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "이미지 업로드 및 수정 성공"),
                    @ApiResponse(responseCode = "400", description = "업로드된 파일이 비어있음"),
                    @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰"),
                    @ApiResponse(responseCode = "404", description = "사용자 정보 없음")
            }
    )
    @PostMapping(value = "/update/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfileImage(HttpServletRequest request,
                                                @RequestPart("file") MultipartFile file) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String userId = jwtTokenProvider.getUsernameFromToken(token);
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String oldImageUrl = user.getProfileImage();
        if (oldImageUrl != null && oldImageUrl.contains("profile/")) {
            s3Uploader.delete(oldImageUrl);
        }

        String newImageUrl = s3Uploader.upload(file);
        user.setProfileImage(newImageUrl);
        userRepository.save(user);

        return ResponseEntity.ok("프로필 이미지가 수정되었습니다.");
    }

    @Operation(summary = "사용자가 관심을 가진 책 조회", description = "사용자가 관심을 가진 책 리스트를 조회합니다.")
    @GetMapping("/{userId}/liked-books")
    public void getLikedBooks() {
    }

    @Operation(summary = "사용자가 원하는 고전 신청", description = "사용자가 원하는 고전을 신청합니다.")
    @PostMapping("/{userId}/requested-books")
    public void requestBook() {
    }
}