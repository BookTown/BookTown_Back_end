package hello.booktown.controller;



import hello.booktown.dto.SummarizeRequest;
import hello.booktown.service.SceneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scenes")
@RequiredArgsConstructor
public class SceneController {

    private final SceneService sceneService;

    @PostMapping("/summarize")
    public ResponseEntity<String> summarizeBook(@RequestBody SummarizeRequest request) {
        sceneService.summarizeBookByChunks(request.getBookId());
        return ResponseEntity.ok("📘 책 요약이 완료되었습니다.");
    }
}
