package hello.booktown.domain;


import hello.booktown.dto.QuizSubmissionHistoryDto;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

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
        return QuizSubmissionHistoryDto.builder()
                .question(this.getQuiz().getQuestion())
                .userAnswer(this.getUserAnswer())
                .correctAnswer(this.getQuiz().getCorrectAnswer())
                .isCorrect(this.isCorrect())
                .build();
    }

}