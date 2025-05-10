package hello.booktown.dto;

import lombok.Data;

@Data
public class QuizSubmissionDto {
    private Long quizId;
    private String answer;
}
