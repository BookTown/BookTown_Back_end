package hello.booktown.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class QuizSubmissionGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Book book;

    @Column(name = "group_index")
    private int groupIndex;


    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "submissionGroup", cascade = CascadeType.ALL)
    private List<QuizSubmission> submissions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.submittedAt = LocalDateTime.now();
    }
}

