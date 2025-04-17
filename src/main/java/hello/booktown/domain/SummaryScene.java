package hello.booktown.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class SummaryScene {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private BookSummary bookSummary;

    private int pageNumber;

    @Lob
    private String content;

    @Column(columnDefinition = "TEXT")
    private String illustrationUrl;
}

