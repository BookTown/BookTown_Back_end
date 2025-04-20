package hello.booktown.controller;

import hello.booktown.service.LikeService;
import hello.booktown.util.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/book/like")
public class LikeController {

    private final LikeService likeService;

    @Operation(summary = "책에 하트 설정", description = "사용자가 책에 하트를 눌러 관심 책으로 설정 및 해제합니다.")
    @PostMapping("/{bookId}")
    public ResponseEntity<Boolean> toggleLike(
            @PathVariable Long bookId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        boolean liked = likeService.toggleLike(bookId, userId);
        return ResponseEntity.ok(liked);
    }
}