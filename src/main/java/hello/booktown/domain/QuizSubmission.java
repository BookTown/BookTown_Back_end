package hello.booktown.domain;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class QuizSubmission {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Quiz quiz;

    @ManyToOne
    private User user;

    private String userAnswer;
    private boolean isCorrect;
}