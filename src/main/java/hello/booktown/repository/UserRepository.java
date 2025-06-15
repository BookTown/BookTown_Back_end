package hello.booktown.repository;

import hello.booktown.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    List<User> findTop3ByOrderByScoreDesc();
    List<User> findAllByOrderByScoreDesc();
}