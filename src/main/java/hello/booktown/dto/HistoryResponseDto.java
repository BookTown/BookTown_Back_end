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
    private Long id;
    private Long bookId;
    private String bookTitle;
    private int score;
    private String submittedAt;
    private int groupIndex;  // 그룹 번호도 포함하고 싶다면 추가
}

