package hello.booktown.service;

import hello.booktown.domain.User;
import hello.booktown.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // provider + providerId로 유저 찾기
    public Optional<User> findByProviderInfo(String provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId);
    }

    // 새로운 유저 저장
    public User registerUser(String email, String provider, String providerId, String nickname, String profileImage) {
        User user = new User(email, provider, providerId, nickname, profileImage);
        return userRepository.save(user);
    }
}
