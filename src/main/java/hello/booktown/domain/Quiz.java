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

    @Enumerated(EnumType.STRING)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    private String question;

    private String correctAnswer;

    private int score; // AI가 난이도 및 정답 기준으로 설정

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    private List<QuizOption> options = new ArrayList<>();
}


