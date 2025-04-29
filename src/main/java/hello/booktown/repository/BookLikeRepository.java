package hello.booktown.repository;

import hello.booktown.domain.Book;
import hello.booktown.domain.BookLike;
import hello.booktown.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookLikeRepository extends JpaRepository<BookLike, Long> {
    Optional<BookLike> findByBookAndUser(Book book, User user);
    List<BookLike> findByUser(User user);
}
