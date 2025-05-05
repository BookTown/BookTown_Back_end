package hello.booktown.repository;

import hello.booktown.domain.Quiz;
import hello.booktown.domain.BookSummary;
import hello.booktown.domain.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByBookSummary(BookSummary bookSummary);

    List<Quiz> findByBookSummaryAndQuestionType(BookSummary summary, QuestionType type);

}
