package hello.booktown.controller;

import hello.booktown.domain.Quiz;
import hello.booktown.domain.enums.QuestionType;
import hello.booktown.dto.BulkQuizSubmissionRequest;
import hello.booktown.dto.QuizSubmissionDto;
import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.service.QuizService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/generate/{bookId}")
    public ResponseEntity<List<Quiz>> generateQuiz(@PathVariable Long bookId,
                                                   @RequestParam QuestionType type,
                                                   @RequestParam(defaultValue = "false") boolean forceCreate,
                                                   HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = Long.parseLong(jwtTokenProvider.getUsernameFromToken(token));
        List<Quiz> quizzes = quizService.createQuizzes(bookId, type, userId, forceCreate);

        // 퀴즈 엔티티만 반환 (BookSummary, Scene, Option 등 제거)
        quizzes.forEach(q -> {
            q.setBookSummary(null);;
        });

        return ResponseEntity.ok(quizzes);
    }

    @PostMapping("/submit/bulk")
    public ResponseEntity<List<Boolean>> submitMultipleQuizzes(@RequestBody BulkQuizSubmissionRequest request,
                                                               HttpServletRequest httpRequest) {
        Long userId = Long.parseLong(jwtTokenProvider.getUsernameFromToken(jwtTokenProvider.resolveToken(httpRequest)));
        List<Boolean> results = quizService.submitMultipleAnswers(userId, request.getAnswers());
        return ResponseEntity.ok(results);
    }

    @PostMapping("/submit/batch")
    public ResponseEntity<List<Boolean>> submitBatch(@RequestBody List<QuizSubmissionDto> submissions,
                                                     HttpServletRequest request) {
        Long userId = Long.parseLong(jwtTokenProvider.getUsernameFromToken(jwtTokenProvider.resolveToken(request)));
        List<Boolean> results = submissions.stream()
                .map(submission -> quizService.submitAnswer(userId, submission.getQuizId(), submission.getAnswer()))
                .toList();
        return ResponseEntity.ok(results);
    }

}
