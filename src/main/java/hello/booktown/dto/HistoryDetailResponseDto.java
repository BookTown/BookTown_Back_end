package hello.booktown.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HistoryDetailResponseDto {
    private Long historyId; // null 가능
    private String bookTitle;
    private int totalScore;
    private String submittedAt;
    private List<QuizSubmissionHistoryDto> submissions;
}