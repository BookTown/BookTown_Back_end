package hello.booktown.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class QuizOption {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JsonIgnore
    private Quiz quiz;

    private String text;

    @Column(name = "option_index")
    private int index;
}
