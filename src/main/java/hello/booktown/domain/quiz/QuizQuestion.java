package hello.booktown.domain.quiz;


import hello.booktown.domain.enums.QuestionType;
import hello.booktown.domain.Book;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quizId;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    private QuestionType questionType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;
}