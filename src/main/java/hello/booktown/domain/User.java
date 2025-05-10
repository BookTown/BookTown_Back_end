package hello.booktown.domain;

import hello.booktown.domain.enums.Difficulty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "providerId"})
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    public User(String email, String provider, String providerId, String username, String profileImage) {
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.username = username;
        this.profileImage = profileImage;
        this.score = 0L; // 기본값
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String provider;

    private String providerId;

    private String username;

    private String profileImage;

    private Long score;

    private String introduction;

    public void updateIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public void updateUsername(String username) {
        this.username = username;
    }

    public void updateScore(Long score) {
        this.score = score;
    }

    public void setProfileImage(String imageUrl) {
        this.profileImage = imageUrl;
    }
}