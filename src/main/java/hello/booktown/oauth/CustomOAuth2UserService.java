package hello.booktown.oauth;

import hello.booktown.domain.User;
import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public CustomOAuth2UserService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);


        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");

        Optional<User> optionalUser = userRepository.findByUsername(email);

        if (optionalUser.isEmpty()) {
            User newUser = new User(email, ""); // 패스워드 없음 (소셜 로그인 유저)
            userRepository.save(newUser);
        }

        String token = jwtTokenProvider.generateToken(email);
        System.out.println("JWT 발급 완료: " + token);

        return oAuth2User;
    }
}