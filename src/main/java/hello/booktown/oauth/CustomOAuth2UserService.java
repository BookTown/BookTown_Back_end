package hello.booktown.oauth;

import hello.booktown.domain.User;
import hello.booktown.dto.TokenDto;
import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    public CustomOAuth2UserService(UserRepository userRepository,
                                   JwtTokenProvider jwtTokenProvider,
                                   StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId = extractProviderId(provider, attributes);
        String email = Optional.ofNullable(extractEmail(provider, attributes))
                .filter(e -> !e.isBlank())
                .orElse(provider + "_" + providerId + "@booktown.local");

        String username = Optional.ofNullable(extractUsername(provider, attributes))
                .filter(str -> !str.isBlank())
                .orElse("소셜사용자");

        String profileImage = Optional.ofNullable(extractProfileImage(provider, attributes))
                .filter(str -> !str.isBlank())
                .orElse("https://booktown.local/default-profile.png");

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> userRepository.save(new User(email, provider, providerId, username, profileImage)));

        TokenDto tokenDto = jwtTokenProvider.generateAllTokens(user.getId().toString());

        redisTemplate.opsForValue().set(
                "RT:" + user.getId(),
                tokenDto.getRefreshToken(),
                jwtTokenProvider.getRefreshExpirationTime(),
                TimeUnit.MILLISECONDS
        );

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("id", user.getId().toString());
        userAttributes.put("provider", user.getProvider());
        userAttributes.put("providerId", user.getProviderId());
        userAttributes.put("email", user.getEmail());
        userAttributes.put("username", user.getUsername());
        userAttributes.put("profileImage", user.getProfileImage());
        userAttributes.put("accessToken", tokenDto.getAccessToken());
        userAttributes.put("refreshToken", tokenDto.getRefreshToken());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                userAttributes,
                "id"
        );
    }

    private String extractProviderId(String provider, Map<String, Object> attributes) {
        switch (provider) {
            case "google": return (String) attributes.get("sub");
            case "kakao": return String.valueOf(attributes.get("id"));
            case "naver":
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                return (String) response.get("id");
            default: return null;
        }
    }

    private String extractEmail(String provider, Map<String, Object> attributes) {
        switch (provider) {
            case "google": return (String) attributes.get("email");
            case "kakao":
                Object accountObj = attributes.get("kakao_account");
                if (accountObj instanceof Map) {
                    Map<String, Object> kakaoAccount = (Map<String, Object>) accountObj;
                    Object emailObj = kakaoAccount.get("email");
                    return emailObj instanceof String ? (String) emailObj : null;
                }
                return null;
            case "naver":
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                return (String) response.get("email");
            default: return null;
        }
    }

    private String extractUsername(String provider, Map<String, Object> attributes) {
        switch (provider) {
            case "google": return (String) attributes.get("name");
            case "kakao":
                Object accountObj = attributes.get("kakao_account");
                if (accountObj instanceof Map) {
                    Map<String, Object> kakaoAccount = (Map<String, Object>) accountObj;
                    Object profileObj = kakaoAccount.get("profile");
                    if (profileObj instanceof Map) {
                        return (String) ((Map<?, ?>) profileObj).get("nickname");
                    }
                }
                return null;
            case "naver":
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                return (String) response.get("nickname");
            default: return null;
        }
    }

    private String extractProfileImage(String provider, Map<String, Object> attributes) {
        switch (provider) {
            case "google": return (String) attributes.get("picture");
            case "kakao":
                Object accountObj = attributes.get("kakao_account");
                if (accountObj instanceof Map) {
                    Map<String, Object> kakaoAccount = (Map<String, Object>) accountObj;
                    Object profileObj = kakaoAccount.get("profile");
                    if (profileObj instanceof Map) {
                        return (String) ((Map<?, ?>) profileObj).get("profile_image_url");
                    }
                }
                return null;
            case "naver":
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                return (String) response.get("profile_image");
            default: return null;
        }
    }
}