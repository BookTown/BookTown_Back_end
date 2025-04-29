package hello.booktown.controller;

import hello.booktown.dto.BookResponse;
import hello.booktown.exception.CustomException;
import hello.booktown.exception.ErrorCode;
import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.service.LikeService;
import hello.booktown.util.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/book/like")
public class LikeController {

    private final LikeService likeService;
    private final JwtTokenProvider jwtTokenProvider;


    @Operation(
            summary = "책에 하트 설정 또는 해제",
            description = "사용자가 책에 하트를 눌러 관심 책으로 설정하거나 해제합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "하트 설정/해제 성공"),
                    @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰"),
                    @ApiResponse(responseCode = "404", description = "책 또는 사용자 정보 없음")
            }
    )
    @PostMapping("/{bookId}")
    public ResponseEntity<Boolean> toggleLike(
            @PathVariable Long bookId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        boolean liked = likeService.toggleLike(bookId, userId);
        return ResponseEntity.ok(liked);
    }

    @Operation(summary = "내가 좋아요 누른 책 목록 조회", description = "로그인한 사용자가 좋아요 누른 책들의 목록을 조회합니다.")
    @GetMapping("/view")
    public ResponseEntity<List<BookResponse>> getLikedBooks(HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String userId = jwtTokenProvider.getUsernameFromToken(token);
        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        List<BookResponse> likedBooks = likeService.getLikedBooksByUser(Long.parseLong(userId));
        return ResponseEntity.ok(likedBooks);
    }
}
