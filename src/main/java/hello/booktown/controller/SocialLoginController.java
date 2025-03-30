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
        String email = oAuth2User.getAttribute("email");

        // 사용자 정보 DB에 존재하는지 확인 후 없으면 저장
        Optional<User> existingUser = userRepository.findByUsername(email);
        if (existingUser.isEmpty()) {
            User newUser = new User(email, "SOCIAL_LOGIN");
            userRepository.save(newUser);
        }

        String token = jwtTokenProvider.generateToken(email);

        // 테스트용 출력 (리디렉션용 아님)
        response.getWriter().write("JWT: " + token);
    }
}