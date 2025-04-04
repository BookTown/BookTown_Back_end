package hello.booktown.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    @Column(nullable = false)
    private String title;

    private String author;
    private String ISBN;
    private Integer publicationYear;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    private List<Scene> scenes;
}
