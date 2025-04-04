package hello.booktown.domain.quiz;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Table(name = "quiz_answers")
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerId;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private QuizQuestion quizQuestion;

    private String answerOptions;
    private String answer;
}