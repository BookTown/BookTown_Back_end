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
import java.util.logging.Logger;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = Logger.getLogger(OAuth2SuccessHandler.class.getName());

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
        try {
            DefaultOAuth2User user = (DefaultOAuth2User) authentication.getPrincipal();
            String userId = (String) user.getAttributes().get("userId");

            logger.info("OAuth2 로그인 성공 - userId: " + userId);

            String accessToken = jwtTokenProvider.generateToken(userId);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

            logger.info("AccessToken 생성 완료");
            logger.info("RefreshToken 생성 완료");

            redisTemplate.opsForValue().set("RT:" + userId, refreshToken,
                    jwtTokenProvider.getRefreshExpirationTime(), TimeUnit.MILLISECONDS);

            logger.info("RefreshToken Redis 저장 완료: RT:" + userId);

            Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge((int) (jwtTokenProvider.getRefreshExpirationTime() / 1000));
            response.addCookie(refreshCookie);

            logger.info("RefreshToken 쿠키 저장 완료");

            String redirectUrl = "https://booktown.site/front/oauth/callback?accessToken=" + accessToken;
            logger.info("프론트로 리디렉션: " + redirectUrl);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            logger.severe("OAuth2SuccessHandler 처리 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "서버 오류 발생");
        }
    }
}