package hello.booktown.oauth;

import hello.booktown.domain.User;
import hello.booktown.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("OAuth2 로그인 시도 - provider: {}", userRequest.getClientRegistration().getRegistrationId());
        OAuth2User oAuth2User;

        try {
            oAuth2User = super.loadUser(userRequest);
            log.debug("받은 attributes: {}", oAuth2User.getAttributes());
        } catch (OAuth2AuthenticationException e) {
            log.error("OAuth2AuthenticationException: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("기타 예외 발생: {}", e.getMessage());
            throw new OAuth2AuthenticationException(new OAuth2Error("load_user_failed"), "사용자 정보를 가져오는 데 실패했습니다.");
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId = extractProviderId(provider, attributes);
        if (providerId == null || providerId.isBlank()) {
            throw new OAuth2AuthenticationException("providerId를 찾을 수 없습니다.");
        }

        String email = Optional.ofNullable(extractEmail(provider, attributes))
                .orElse(provider + "_" + providerId + "@booktown.local");
        String username = Optional.ofNullable(extractUsername(provider, attributes)).orElse("소셜사용자");

        // 프로필 이미지는 무조건 null로 저장
        String profileImage = null;

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    log.info("신규 사용자 등록: {}", email);
                    return userRepository.save(new User(email, provider, providerId, username, profileImage));
                });

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("userId", user.getId().toString());

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
            log.error("providerId 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    private String extractEmail(String provider, Map<String, Object> attributes) {
        try {
            if ("kakao".equals(provider)) {
                Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
                return (String) account.get("email");
            } else if ("google".equals(provider)) {
                return (String) attributes.get("email");
            } else if ("naver".equals(provider)) {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                return (String) response.get("email");
            }
        } catch (Exception e) {
            log.warn("이메일 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private String extractUsername(String provider, Map<String, Object> attributes) {
        try {
            if ("kakao".equals(provider)) {
                Map<String, Object> profile = (Map<String, Object>) ((Map<String, Object>) attributes.get("kakao_account")).get("profile");
                return (String) profile.get("nickname");
            } else if ("google".equals(provider)) {
                return (String) attributes.get("name");
            } else if ("naver".equals(provider)) {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                return (String) response.get("nickname");
            }
        } catch (Exception e) {
            log.warn("사용자명 추출 실패: {}", e.getMessage());
        }
        return null;
    }
}