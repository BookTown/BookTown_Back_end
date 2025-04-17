package hello.booktown.repository;

import hello.booktown.domain.Book;
import hello.booktown.domain.BookSummary;
import hello.booktown.domain.SummaryScene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SummarySceneRepository extends JpaRepository<SummaryScene, Long> {
    List<SummaryScene> findByBookSummary(BookSummary bookSummary);
}
