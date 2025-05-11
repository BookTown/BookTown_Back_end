package hello.booktown.domain;


import hello.booktown.domain.enums.QuestionType;
import hello.booktown.dto.QuizSubmissionHistoryDto;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class QuizSubmission {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToOne
    private User user;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        this.submittedAt = LocalDateTime.now();
    }

    private String userAnswer;
    private boolean isCorrect;

    public QuizSubmissionHistoryDto toHistoryDto() {
        Quiz quiz = this.getQuiz();

        List<String> options = quiz.getQuestionType() == QuestionType.MULTIPLE_CHOICE
                ? quiz.getOptions().stream().map(QuizOption::getText).toList()
                : null;

        return QuizSubmissionHistoryDto.builder()
                .question(quiz.getQuestion())
                .userAnswer(this.getUserAnswer())
                .correctAnswer(quiz.getCorrectAnswer())
                .isCorrect(this.isCorrect())
                .score(quiz.getScore())
                .options(options)
                .build();
    }
}