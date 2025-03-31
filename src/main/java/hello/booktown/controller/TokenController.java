package hello.booktown.controller;

import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.service.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/token")
public class TokenController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public TokenController(JwtTokenProvider jwtTokenProvider, RefreshTokenService refreshTokenService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/reissue")
    public ResponseEntity<?> reissueAccessToken(@RequestHeader("Refresh-Token") String refreshToken) {

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body("유효하지 않은 리프레시 토큰입니다.");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        return refreshTokenService.getRefreshToken(username)
                .filter(savedToken -> savedToken.equals(refreshToken))
                .map(token -> {
                    String newAccessToken = jwtTokenProvider.generateToken(username);
                    String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

                    refreshTokenService.saveRefreshToken(
                            username,
                            newRefreshToken,
                            jwtTokenProvider.getRefreshTokenRemainingMillis(newRefreshToken)
                    );

                    return ResponseEntity.ok(Map.of(
                            "accessToken", "Bearer " + newAccessToken,
                            "refreshToken", "Bearer " + newRefreshToken
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of(
                        "error", "리프레시 토큰 정보가 존재하지 않습니다."
                )));
    }
}