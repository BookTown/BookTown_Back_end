package hello.booktown.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookResponse {

    private Long bookId;
    private String title;
    private String author;
    private String summaryUrl;
    private String thumbnailUrl;
    private int likecount;
}

