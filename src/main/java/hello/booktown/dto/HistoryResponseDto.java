package hello.booktown.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryResponseDto {
    private Long id; // null 가능
    private Long bookId;
    private String bookTitle;
    private int score;
    private String submittedAt;

}
