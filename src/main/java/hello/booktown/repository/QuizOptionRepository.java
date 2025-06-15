package hello.booktown.repository;

import hello.booktown.domain.QuizOption;
import hello.booktown.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizOptionRepository extends JpaRepository<QuizOption, Long> {
    List<QuizOption> findByQuiz(Quiz quiz);

    void deleteByQuizIn(List<Quiz> quizzes);
}
