package hello.booktown.repository;

import hello.booktown.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @Query(value = "SELECT * FROM books ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Book> findRandomBook();

    // 2. 최신 등록 순 10개
    List<Book> findTop10ByOrderByCreatedAtDesc();

    // 3. 좋아요 순 10개
    List<Book> findTop10ByOrderByLikeCountDesc();


    List<Book> findAllByOrderByLikeCountDesc();

    List<Book> findAllByOrderByCreatedAtDesc();

}