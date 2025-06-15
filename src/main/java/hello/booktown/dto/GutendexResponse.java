package hello.booktown.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GutendexResponse {
    private String title;
    private List<Author> authors;
    private Map<String, String> formats;

    @Data
    public static class Author {
        private String name;
    }

}
