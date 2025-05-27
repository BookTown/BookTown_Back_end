package hello.booktown.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class SummaryScene {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ 외래 키 컬럼 명시 (중요)
    @ManyToOne
    @JoinColumn(name = "book_summary_id", nullable = false)
    @JsonBackReference
    private BookSummary bookSummary;

    private int pageNumber;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String illustrationUrl;

    @Column(name = "female_audio_url", columnDefinition = "TEXT")
    private String femaleAudioUrl;

    @Column(name = "male_audio_url", columnDefinition = "TEXT")
    private String maleAudioUrl;

    // Getter for default audio url (female)
    public String getAudioUrl() {
        return femaleAudioUrl;
    }

    public String getFemaleAudioUrl() {
        return femaleAudioUrl;
    }

    public String getMaleAudioUrl() {
        return maleAudioUrl;
    }
}
