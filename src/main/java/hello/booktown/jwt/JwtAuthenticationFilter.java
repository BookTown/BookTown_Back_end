package hello.booktown.jwt;

import hello.booktown.util.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, StringRedisTemplate redisTemplate) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String auth = request.getHeader("Authorization");

        System.out.println("=== JWT 필터 실행 ===");
        System.out.println("Request URI: " + uri);
        System.out.println("Authorization Header: " + auth);

        if (uri.startsWith("/swagger-ui") ||
                uri.contains("swagger") ||
                uri.equals("/swagger-ui.html") ||
                uri.startsWith("/v3/api-docs") ||
                uri.equals("/v3/api-docs/swagger-config") ||
                uri.startsWith("/swagger-resources") ||
                uri.startsWith("/webjars") ||
                uri.endsWith(".js") ||
                uri.endsWith(".css") ||
                uri.endsWith(".html") ||
                uri.endsWith(".png") ||
                uri.endsWith(".ico") ||
                uri.endsWith(".map") ||
                uri.equals("/") ||
                uri.equals("/index.html")) {
            System.out.println(">> JWT 필터 예외 처리: 필터 통과");
            filterChain.doFilter(request, response);
            return;
        }

        String token = jwtTokenProvider.resolveToken(request);

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            System.out.println(">> 유효한 JWT 없음. 필터 통과");
            filterChain.doFilter(request, response);
            return;
        }

        // 로그아웃된 토큰인지 확인
        String isLoggedOut = redisTemplate.opsForValue().get(token);
        if ("logout".equals(isLoggedOut)) {
            System.out.println(">> 로그아웃된 토큰. 필터 통과");
            filterChain.doFilter(request, response);
            return;
        }

        String userId = jwtTokenProvider.getUsernameFromToken(token);
        if (userId != null && userId.matches("\\d+")) {
            Long parsedUserId = Long.parseLong(userId);
            CustomUserDetails customUserDetails = new CustomUserDetails(parsedUserId, "unknown@booktown.local", "ROLE_USER");

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            System.out.println(">> 인증 객체 설정 완료: userId = " + userId);
        }

        filterChain.doFilter(request, response);
    }
}