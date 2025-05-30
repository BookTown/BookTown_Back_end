package hello.booktown.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hello.booktown.domain.enums.ApplyStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookApply {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private ApplyStatus status;

    private LocalDateTime appliedAt; // 신청일

    private String rejectionReason; // 거절 사유 (nullable)
}