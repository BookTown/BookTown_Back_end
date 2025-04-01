package hello.booktown.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    @Operation(summary = "퀴즈 생성", description = "주어진 책에 대해 퀴즈를 생성합니다. 퀴즈 유형을 선택할 수 있습니다.")
    @PostMapping("/{bookName}/category")
    public void createQuiz(@PathVariable String bookName, @RequestParam String category) {
        // 구현 내용
    }

    @Operation(summary = "퀴즈 제출", description = "사용자가 퀴즈를 제출하고 채점 결과를 반환합니다.")
    @PostMapping("/submit")
    public void submitQuiz(@RequestBody String submission) {
        // 구현 내용
    }
}
