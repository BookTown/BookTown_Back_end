package hello.booktown.oauth;

import hello.booktown.domain.User;
import hello.booktown.domain.enums.UserRole;
import hello.booktown.repository.UserRepository;
import hello.booktown.util.CustomUserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User;
        try {
            oAuth2User = super.loadUser(userRequest);
        } catch (Exception e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("load_user_failed"), "사용자 정보 로딩 실패");
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId = extractProviderId(provider, attributes);
        if (providerId == null || providerId.isBlank()) {
            throw new OAuth2AuthenticationException("providerId가 없습니다.");
        }

        String email = Optional.ofNullable(extractEmail(provider, attributes))
                .orElse(provider + "_" + providerId + "@booktown.local");
        String username = Optional.ofNullable(extractUsername(provider, attributes)).orElse("소셜사용자");
        String profileImage = null;

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> userRepository.save(new User(email, provider, providerId, username, profileImage, UserRole.USER)));

        // CustomUserDetails 생성
        CustomUserDetails customUserDetails = new CustomUserDetails(user.getId(), user.getEmail(), "ROLE_USER");

        // OAuth2User로 반환, CustomUserDetails 정보를 attributes에 포함
        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("userId", user.getId().toString());
        userAttributes.put("email", user.getEmail());
        userAttributes.put("role", "ROLE_USER"); // 이 역할 정보도 사용됨

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                userAttributes,
                "userId"
        );
    }



    private String extractProviderId(String provider, Map<String, Object> attributes) {
        try {
            return switch (provider) {
                case "google" -> (String) attributes.get("sub");
                case "kakao" -> String.valueOf(attributes.get("id"));
                case "naver" -> {
                    Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                    yield (String) response.get("id");
                }
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private String extractEmail(String provider, Map<String, Object> attributes) {
        try {
            return switch (provider) {
                case "google" -> (String) attributes.get("email");
                case "kakao" -> {
                    Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
                    yield (String) account.get("email");
                }
                case "naver" -> {
                    Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                    yield (String) response.get("email");
                }
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private String extractUsername(String provider, Map<String, Object> attributes) {
        try {
            return switch (provider) {
                case "google" -> (String) attributes.get("name");
                case "kakao" -> {
                    Map<String, Object> profile = (Map<String, Object>) ((Map<String, Object>) attributes.get("kakao_account")).get("profile");
                    yield (String) profile.get("nickname");
                }
                case "naver" -> {
                    Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                    yield (String) response.get("nickname");
                }
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }
}