package hello.booktown.domain;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private ApplyStatus status;

    private LocalDateTime appliedAt; // 신청일

    private String rejectionReason; // 거절 사유 (nullable)
}