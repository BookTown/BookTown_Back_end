package hello.booktown.repository;

import hello.booktown.domain.BookApply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookApplyRepository extends JpaRepository<BookApply, Long> {
    List<BookApply> findByUserIdOrderByAppliedAtDesc(Long userId);
}