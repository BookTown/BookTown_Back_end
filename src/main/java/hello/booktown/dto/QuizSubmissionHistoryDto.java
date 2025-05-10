package hello.booktown.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizSubmissionHistoryDto {
    private String question;
    private String userAnswer;
    private String correctAnswer;
    private boolean isCorrect;
    private int score;

}