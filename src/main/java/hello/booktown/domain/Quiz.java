package hello.booktown.domain;

import hello.booktown.domain.enums.Difficulty;
import hello.booktown.domain.enums.QuestionType;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Quiz {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private BookSummary bookSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    private String question;

    private String correctAnswer;

    private int score;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    private List<QuizOption> options = new ArrayList<>();
}
