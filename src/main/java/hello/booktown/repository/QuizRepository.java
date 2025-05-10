package hello.booktown.repository;

import hello.booktown.domain.Book;
import hello.booktown.domain.Quiz;
import hello.booktown.domain.BookSummary;
import hello.booktown.domain.User;
import hello.booktown.domain.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    @Modifying
    @Query("DELETE FROM Quiz q WHERE q.user = :user AND q.bookSummary.book = :book")
    void deleteByUserAndBook(@Param("user") User user, @Param("book") Book book);

    List<Quiz> findByBookSummaryAndQuestionTypeAndUser(BookSummary summary, QuestionType type, User user);


}
