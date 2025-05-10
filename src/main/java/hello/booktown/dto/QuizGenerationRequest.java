package hello.booktown.dto;

import hello.booktown.domain.enums.Difficulty;
import hello.booktown.domain.enums.QuestionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizGenerationRequest {
    private Long bookId;
    private QuestionType type;
    private Difficulty difficulty;
}