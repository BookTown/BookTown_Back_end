package hello.booktown.controller;

import hello.booktown.domain.User;
import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RestController
public class SocialLoginController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public SocialLoginController(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Operation(summary = "소셜 로그인 성공 처리", description = "OAuth2 로그인 성공 후 사용자 등록 및 JWT 토큰 발급 처리.")
    @GetMapping("/login/success")
    public void loginSuccess(@AuthenticationPrincipal OAuth2User oAuth2User,
                             HttpServletResponse response) throws IOException {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String provider = oAuth2User.getAttribute("provider"); // 커스텀 필드 처리 안 되어 있으면 추출 불가

        // provider 추출 (대체 방법)
        String providerName = oAuth2User.getAuthorities().toString().toLowerCase().contains("google") ? "google" :
                oAuth2User.getAttributes().containsKey("kakao_account") ? "kakao" :
                        oAuth2User.getAttributes().containsKey("response") ? "naver" : "unknown";

        String providerId = extractProviderId(providerName, attributes);
        String email = extractEmail(providerName, attributes);

        Optional<User> existingUser = userRepository.findByProviderAndProviderId(providerName, providerId);

        if (existingUser.isEmpty()) {
            String nickname = extractNickname(providerName, attributes);
            String profileImage = extractProfileImage(providerName, attributes);
            User newUser = new User(email, providerName, providerId, nickname, profileImage);
            userRepository.save(newUser);
        }

        String token = jwtTokenProvider.generateToken(email);

        response.getWriter().write("JWT: " + token);
    }

    private String extractProviderId(String provider, Map<String, Object> attributes) {
        switch (provider) {
            case "google": return (String) attributes.get("sub");
            case "kakao": return String.valueOf(attributes.get("id"));
            case "naver": return ((Map<String, Object>) attributes.get("response")).get("id").toString();
            default: return null;
        }
    }

    private String extractEmail(String provider, Map<String, Object> attributes) {
        switch (provider) {
            case "google": return (String) attributes.get("email");
            case "kakao": return (String) ((Map<String, Object>) attributes.get("kakao_account")).get("email");
            case "naver": return (String) ((Map<String, Object>) attributes.get("response")).get("email");
            default: return null;
        }
    }

    private String extractNickname(String provider, Map<String, Object> attributes) {
        switch (provider) {
            case "google": return (String) attributes.get("name");
            case "kakao":
                Map<String, Object> kakaoProfile = (Map<String, Object>) ((Map<String, Object>) attributes.get("kakao_account")).get("profile");
                return (String) kakaoProfile.get("nickname");
            case "naver":
                return (String) ((Map<String, Object>) attributes.get("response")).get("nickname");
            default: return null;
        }
    }

    private String extractProfileImage(String provider, Map<String, Object> attributes) {
        switch (provider) {
            case "google": return (String) attributes.get("picture");
            case "kakao":
                Map<String, Object> kakaoProfile = (Map<String, Object>) ((Map<String, Object>) attributes.get("kakao_account")).get("profile");
                return (String) kakaoProfile.get("profile_image_url");
            case "naver":
                return (String) ((Map<String, Object>) attributes.get("response")).get("profile_image");
            default: return null;
        }
    }
}