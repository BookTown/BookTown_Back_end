package hello.booktown.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class SummaryScene {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private BookSummary bookSummary;

    private int pageNumber;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String illustrationUrl;
}

