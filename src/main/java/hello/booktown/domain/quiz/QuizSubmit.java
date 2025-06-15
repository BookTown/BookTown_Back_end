package hello.booktown.domain.quiz;

import hello.booktown.domain.Book;
import hello.booktown.domain.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "quiz_submit")
public class QuizSubmit {

    @Id
    private String submitId;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private QuizQuestion quizQuestion;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String userAnswer;

    private Boolean isCorrect;
    private Integer score;
}