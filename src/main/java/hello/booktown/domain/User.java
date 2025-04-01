package hello.booktown.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class User {

    public User(String email, String provider, String providerId, String nickname, String profileImage) {
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.nickname = nickname;
        this.profileImage = profileImage;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email; // 사용자 이메일 (유일)

    private String provider; // ex) google, kakao, naver

    private String providerId; // 소셜 플랫폼 고유 ID

    private String nickname; // 사용자 이름 or 별명

    private String profileImage; // 프로필 이미지 URL
}