package hello.booktown.controller;

import hello.booktown.dto.UserLoginRequest;
import hello.booktown.dto.UserRegisterRequest;
import hello.booktown.domain.User;
import hello.booktown.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public User register(@RequestBody UserRegisterRequest request) {
        return userService.register(request.getUsername(), request.getPassword());
    }

    @PostMapping("/login")
    public User login(@RequestBody UserLoginRequest request) {
        return userService.login(request.getUsername(), request.getPassword())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 틀렸습니다."));
    }
}