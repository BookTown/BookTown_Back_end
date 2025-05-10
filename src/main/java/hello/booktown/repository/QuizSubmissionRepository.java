package hello.booktown.repository;

import hello.booktown.domain.QuizSubmission;
import hello.booktown.domain.Quiz;
import hello.booktown.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {
    List<QuizSubmission> findByUser(User user);
    List<QuizSubmission> findByQuiz(Quiz quiz);
    boolean existsByUserAndQuiz(User user, Quiz quiz);
}
