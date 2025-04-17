package hello.booktown.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class BookSummary {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Book book;

    @Lob
    private String fullSummary;

    @OneToMany(mappedBy = "bookSummary", cascade = CascadeType.ALL)
    private List<SummaryScene> scenes = new ArrayList<>();
}
