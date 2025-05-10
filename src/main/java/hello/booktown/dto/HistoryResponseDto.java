package hello.booktown.dto;

import hello.booktown.domain.History;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HistoryResponseDto {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private int score;
    private String submittedAt;

    public static HistoryResponseDto fromEntity(History entity) {
        return HistoryResponseDto.builder()
                .id(entity.getId())
                .bookId(entity.getBook().getId())
                .bookTitle(entity.getBook().getTitle())
                .score(entity.getScore())
                .submittedAt(entity.getSubmittedAt().toString())
                .build();
    }
}
