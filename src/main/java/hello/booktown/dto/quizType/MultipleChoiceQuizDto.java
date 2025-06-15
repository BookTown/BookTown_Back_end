package hello.booktown.dto.quizType;

import lombok.Data;

import java.util.List;

@Data
public class MultipleChoiceQuizDto {
    private String type;
    private String difficulty;
    private String question;
    private List<String> options;
    private int answerIndex;
    private int score;
    private String explanation;
}
