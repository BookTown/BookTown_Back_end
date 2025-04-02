package hello.booktown.oauth;

import hello.booktown.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    public OAuth2SuccessHandler(JwtTokenProvider jwtTokenProvider,
                                StringRedisTemplate redisTemplate) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
        String userId = (String) oAuth2User.getAttributes().get("id");

        // 토큰 생성
        String accessToken = jwtTokenProvider.generateToken(userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // Redis 저장
        redisTemplate.opsForValue().set(
                "RT:" + userId,
                refreshToken,
                jwtTokenProvider.getRefreshExpirationTime(),
                TimeUnit.MILLISECONDS
        );

        // Refresh 토큰을 쿠키에 저장
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) jwtTokenProvider.getRefreshExpirationTime() / 1000);
        response.addCookie(cookie);

        // 프론트로 리다이렉트
        String redirectUrl = String.format("https://booktown.site/front/oauth/callback?accessToken=%s&refreshToken=%s", accessToken, "httpOnly");
        response.sendRedirect(redirectUrl);
    }
}