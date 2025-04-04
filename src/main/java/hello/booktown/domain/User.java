package hello.booktown.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

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

    private String provider;     // google, kakao 등

    private String providerId;   // 소셜 고유 ID

    private String username;

    private String profileImage;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt; //업데이트 컨트롤러나 서비스 만들고 구현하기

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLogin;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastLogin = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = new Date();
        this.lastLogin = new Date();
    }

    public void updateLastLogin() {
        this.lastLogin = new Date();
    }
}
