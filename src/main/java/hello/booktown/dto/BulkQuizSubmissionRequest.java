package hello.booktown.dto;

import lombok.Data;

import java.util.List;

@Data
public class BulkQuizSubmissionRequest {
    private List<QuizAnswer> answers;

    @Data
    public static class QuizAnswer {
        private Long quizId;
        private String answer;
    }
}
