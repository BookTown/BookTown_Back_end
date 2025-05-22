package hello.booktown.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroupedHistoryDto {
    private Long bookId;
    private String title;
    private String author;
    private List<HistoryResponseDto> histories;
    private int totalScore;
}
