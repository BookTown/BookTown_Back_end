package hello.booktown.controller;

import hello.booktown.domain.Quiz;
import hello.booktown.dto.QuizGenerationRequest;
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

    @PostMapping("/generate")
    public ResponseEntity<List<Quiz>> generateQuiz(@RequestBody QuizGenerationRequest request,
                                                   HttpServletRequest httpRequest) {
        String token = jwtTokenProvider.resolveToken(httpRequest);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = Long.parseLong(jwtTokenProvider.getUsernameFromToken(token));

        List<Quiz> quizzes = quizService.createQuizzes(
                request.getBookId(),
                request.getType(),
                request.getDifficulty(),
                userId
        );

        quizzes.forEach(q -> {
            q.setBookSummary(null);
            q.setUser(null);
        });

        return ResponseEntity.ok(quizzes);
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
