package hello.booktown.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryDetailResponseDto {
    private Long historyId;
    private String bookTitle;
    private int totalScore;
    private String submittedAt;
    private List<QuizSubmissionHistoryDto> submissions;
    private int groupIndex;  // 선택사항
}
