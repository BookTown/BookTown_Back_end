package hello.booktown.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizSubmissionHistoryDto {
    private String question;
    private String userAnswer;
    private String correctAnswer;
    private boolean isCorrect;
    private int score;
    private String explanation;
    private List<String> options;

}