package hello.booktown.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class BookSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "book_id", nullable = false)
    @JsonBackReference
    private Book book;

    @OneToMany(mappedBy = "bookSummary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SummaryScene> scenes = new ArrayList<>();
}
