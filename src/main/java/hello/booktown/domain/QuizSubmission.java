package hello.booktown.domain;

import hello.booktown.domain.enums.QuestionType;
import hello.booktown.dto.QuizSubmissionHistoryDto;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class QuizSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private QuizSubmissionGroup submissionGroup;

    private String userAnswer;

    private boolean isCorrect;

    public QuizSubmissionHistoryDto toHistoryDto() {
        Quiz quiz = this.getQuiz();

        List<String> options = quiz.getQuestionType() == QuestionType.MULTIPLE_CHOICE
                ? quiz.getOptions().stream().map(QuizOption::getText).toList()
                : null;

        return QuizSubmissionHistoryDto.builder()
                .question(quiz.getQuestion())
                .userAnswer(this.userAnswer)
                .correctAnswer(quiz.getCorrectAnswer())
                .isCorrect(this.isCorrect)
                .score(quiz.getScore())
                .explanation(quiz.getExplanation())
                .options(options)
                .build();
    }
}
