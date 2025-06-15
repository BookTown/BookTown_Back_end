package hello.booktown.repository;

import hello.booktown.domain.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {
}
