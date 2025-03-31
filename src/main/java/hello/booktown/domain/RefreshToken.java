package hello.booktown.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class RefreshToken {

    public RefreshToken(String username, String token) {
        this.username = username;
        this.token = token;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(nullable = false)
    private String token;

    public void updateToken(String newToken) {
        this.token = newToken;
    }
}