package hello.booktown.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummarySceneResponse {

    private int pageNumber;
    private String content;
    private String illustrationUrl;
}
