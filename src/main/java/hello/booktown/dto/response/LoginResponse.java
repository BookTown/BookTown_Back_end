package hello.booktown.dto.response;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class LoginResponse {
    private String token;

    public LoginResponse(String token) {
        this.token = token;
    }
}
