package hello.booktown.repository;

import hello.booktown.domain.Book;
import hello.booktown.domain.BookSummary;
import hello.booktown.domain.SummaryScene;
import hello.booktown.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookSummaryRepository extends JpaRepository<BookSummary, Long> {

    boolean existsByBookId(Long bookId);

    Optional<BookSummary> findByBookId(Long bookId);

}
