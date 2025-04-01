package hello.booktown.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/book/like")
public class LikeController {

    @Operation(summary = "책에 하트 설정", description = "사용자가 책에 하트를 눌러 관심 책으로 설정합니다.")
    @PostMapping("/{bookId}")
    public void likeBook(@PathVariable Long bookId) {

    }

    @Operation(summary = "책에 하트 해제", description = "사용자가 책에 하트를 눌러 관심 책을 해제합니다.")
    @DeleteMapping("/{bookId}")
    public void unlikeBook(@PathVariable Long bookId) {

    }
}
