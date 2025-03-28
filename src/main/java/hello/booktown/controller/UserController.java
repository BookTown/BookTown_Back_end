package hello.booktown.controller;

import hello.booktown.domain.User;
import hello.booktown.dto.UserLoginRequest;
import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserLoginRequest request) {
        userService.register(request.getUsername(), request.getPassword());
        return ResponseEntity.ok("회원가입 완료");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequest request) {
        return userService.login(request.getUsername(), request.getPassword())
                .map(user -> {
                    String token = jwtTokenProvider.generateToken(user.getUsername());
                    return ResponseEntity.ok().body(token);
                })
                .orElseGet(() ->
                        ResponseEntity.status(401).body("아이디 또는 비밀번호가 틀렸습니다.")
                );
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok().body("현재 로그인한 사용자: " + principal);
    }
}