package hello.booktown.repository;

import hello.booktown.domain.BookApply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookApplyRepository extends JpaRepository<BookApply, Long> {
}