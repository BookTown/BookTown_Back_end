package hello.booktown.controller;

import hello.booktown.service.LikeService;
import hello.booktown.util.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/book/like")
public class LikeController {

    private final LikeService likeService;

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
}
