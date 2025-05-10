package hello.booktown.repository;

import hello.booktown.domain.History;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistoryRepository extends JpaRepository<History, Long> {

    // 특정 유저의 전체 히스토리 목록 조회
    List<History> findByUserId(Long userId);

    // 특정 히스토리 ID가 해당 유저의 것인지 확인하며 조회
    Optional<History> findByIdAndUserId(Long historyId, Long userId);
}
