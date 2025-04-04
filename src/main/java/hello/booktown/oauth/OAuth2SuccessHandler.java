package hello.booktown.oauth;

import hello.booktown.jwt.JwtTokenProvider;
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

    public OAuth2SuccessHandler(JwtTokenProvider jwtTokenProvider, StringRedisTemplate redisTemplate) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        DefaultOAuth2User user = (DefaultOAuth2User) authentication.getPrincipal();
        String userId = (String) user.getAttributes().get("userId");

        String accessToken = jwtTokenProvider.generateToken(userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        redisTemplate.opsForValue().set("RT:" + userId, refreshToken,
                jwtTokenProvider.getRefreshExpirationTime(), TimeUnit.MILLISECONDS);

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) (jwtTokenProvider.getRefreshExpirationTime() / 1000));
        response.addCookie(refreshCookie);

        System.out.println("로그인 성공: userId = " + userId);
        System.out.println("AccessToken = " + accessToken);
        System.out.println("RefreshToken = " + refreshToken);

        String redirectUrl = "https://booktown.site/front/oauth/callback?accessToken=" + accessToken;
        response.sendRedirect(redirectUrl);
    }
}