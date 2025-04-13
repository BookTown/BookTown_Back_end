package hello.booktown.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class BookSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Book book;

    @Lob
    private String summaryJson; // 10개 문단 요약을 JSON 배열로 저장

    private boolean isDefault; // 최초 자동 생성된 기본 요약인지 (관리자 요약 등)
}
