package hello.booktown.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BookSummaryResponse {

    private Long bookId;
    private String title;
    private String fullSummary;
    private List<SummarySceneResponse> scenes;
}
