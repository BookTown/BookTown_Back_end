package hello.booktown.repository;

import hello.booktown.domain.Book;
import hello.booktown.domain.Scene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SceneRepository extends JpaRepository<Scene, Long> {
    List<Scene> findByBook(Book book);
}
