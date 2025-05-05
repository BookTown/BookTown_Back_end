package hello.booktown.dto.quizType;

import lombok.Data;

@Data
public class ShortAnswerQuizDto {
    private String type;
    private String difficulty;
    private String question;
    private String answer;
    private int score;
}
