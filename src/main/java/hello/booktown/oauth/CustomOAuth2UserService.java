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

        String provider = userRequest.getClientRegistration().getRegistrationId(); // ex. google, kakao, naver
        String providerId = extractProviderId(provider, attributes);
        String email = extractEmail(provider, attributes);
        String nickname = extractNickname(provider, attributes);
        String profileImage = extractProfileImage(provider, attributes);

        Optional<User> optionalUser = userRepository.findByProviderAndProviderId(provider, providerId);

        if (optionalUser.isEmpty()) {
            User newUser = new User(email, provider, providerId, nickname, profileImage);
            userRepository.save(newUser);
        }

        String token = jwtTokenProvider.generateToken(email);
        System.out.println("JWT 발급 완료: " + token);

        return oAuth2User;
    }

    private String extractProviderId(String provider, Map<String, Object> attributes) {
        switch (provider) {
            case "google":
                return (String) attributes.get("sub");
            case "kakao":
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");
                return String.valueOf(attributes.get("id")); // 카카오 고유 ID
            case "naver":
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                return (String) response.get("id");
            default:
                throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다: " + provider);
        }
    }

    private String extractEmail(String provider, Map<String, Object> attributes) {
        switch (provider) {
            case "google":
                return (String) attributes.get("email");
            case "kakao":
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                return (String) kakaoAccount.get("email");
            case "naver":
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                return (String) response.get("email");
            default:
                return null;
        }
    }

    private String extractNickname(String provider, Map<String, Object> attributes) {
        switch (provider) {
            case "google":
                return (String) attributes.get("name");
            case "kakao":
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");
                return (String) kakaoProfile.get("nickname");
            case "naver":
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                return (String) response.get("nickname");
            default:
                return null;
        }
    }

    private String extractProfileImage(String provider, Map<String, Object> attributes) {
        switch (provider) {
            case "google":
                return (String) attributes.get("picture");
            case "kakao":
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");
                return (String) kakaoProfile.get("profile_image_url");
            case "naver":
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                return (String) response.get("profile_image");
            default:
                return null;
        }
    }
}