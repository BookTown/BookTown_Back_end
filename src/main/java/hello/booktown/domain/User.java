package hello.booktown.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String provider;

    private String providerId;

    private String username;

    private String profileImage;
}