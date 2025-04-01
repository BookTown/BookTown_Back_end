package hello.booktown.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/history")
public class HistoryController {

    @Operation(summary = "전체 퀴즈 히스토리 조회", description = "사용자 전체 퀴즈 히스토리를 조회합니다. 퀴즈 결과와 책 정보를 포함합니다.")
    @GetMapping("/{userId}")
    public void getAllHistory(@PathVariable Long userId) {
        // 구현 내용
    }

    @Operation(summary = "특정 퀴즈 히스토리 조회", description = "특정 퀴즈에 대한 결과를 조회합니다. 제출 내역 및 점수 확인이 가능합니다.")
    @GetMapping("/{userId}/{quizHistoryId}")
    public void getQuizHistory(@PathVariable Long userId, @PathVariable Long quizHistoryId) {
        // 구현 내용
    }

    @Operation(summary = "퀴즈 히스토리 삭제", description = "사용자가 푼 퀴즈의 히스토리를 삭제합니다.")
    @DeleteMapping("/{userId}/{quizHistoryId}")
    public void deleteQuizHistory(@PathVariable Long userId, @PathVariable Long quizHistoryId) {
        // 구현 내용
    }
}
