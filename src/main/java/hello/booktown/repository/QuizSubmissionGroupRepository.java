package hello.booktown.repository;

import hello.booktown.domain.Book;
import hello.booktown.domain.QuizSubmissionGroup;
import hello.booktown.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizSubmissionGroupRepository extends JpaRepository<QuizSubmissionGroup, Long> {
    int countByUserAndBook(User user, Book book);
    List<QuizSubmissionGroup> findByUserIdAndBookId(Long userId, Long bookId);
    Optional<QuizSubmissionGroup> findByUserIdAndBookIdAndGroupIndex(Long userId, Long bookId, int groupIndex);
    void deleteByUserIdAndBookIdAndGroupIndex(Long userId, Long bookId, int groupIndex);

    List<QuizSubmissionGroup> findByUser_Id(Long userId);
}
