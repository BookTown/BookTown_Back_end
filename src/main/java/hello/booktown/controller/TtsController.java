package hello.booktown.controller;

import java.util.Comparator;
import java.util.stream.Collectors;

import java.util.List;

import hello.booktown.repository.SummarySceneRepository;
import org.springframework.web.bind.annotation.GetMapping;
import hello.booktown.domain.BookSummary;
import hello.booktown.repository.BookSummaryRepository;
import hello.booktown.service.TtsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import hello.booktown.domain.SummaryScene;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tts")
public class TtsController {

    private final TtsService ttsService;
    private final BookSummaryRepository bookSummaryRepository;
    private final SummarySceneRepository summarySceneRepository;

    @PostMapping("/summary/{bookId}")
    public ResponseEntity<String> generateTtsFromSummary(@PathVariable Long bookId) {
        BookSummary summary = bookSummaryRepository.findByBookId(bookId)
                .orElseThrow(() -> new IllegalArgumentException("해당 책 요약이 존재하지 않습니다."));

        String text = summary.getScenes().stream()
            .sorted(Comparator.comparingInt(scene -> scene.getPageNumber()))
            .map(SummaryScene::getContent)
            .collect(Collectors.joining(" "));
        String audioUrl = ttsService.generateAndUploadTts(text, bookId, 0);

        return ResponseEntity.ok(audioUrl);
    }

    @GetMapping("/summary/{summaryId}/audio")
    public ResponseEntity<List<String>> getAllAudioUrls(@PathVariable Long summaryId) {
        List<SummaryScene> scenes = summarySceneRepository.findByBookSummaryIdOrderByPageNumberAsc(summaryId);
        List<String> audioUrls = scenes.stream()
                .map(SummaryScene::getAudioUrl)
                .toList();
        return ResponseEntity.ok(audioUrls);
    }

    @GetMapping("/summary/{summaryId}/audio/{pageNumber}")
    public ResponseEntity<String> getAudioUrlByPage(@PathVariable Long summaryId, @PathVariable int pageNumber) {
        SummaryScene scene = summarySceneRepository.findByBookSummaryIdAndPageNumber(summaryId, pageNumber);
        if (scene == null || scene.getAudioUrl() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(scene.getAudioUrl());
    }

    @PostMapping("/test")
    public ResponseEntity<String> testTtsGeneration() {
        String testText = "안녕하세요. 북타운에 오신 것을 환영합니다.";
        Long dummyBookId = 999L; // 테스트용 bookId (실제 저장에는 사용되지 않아도 됨)
        int dummyPageNumber = 0;

        String audioUrl = ttsService.generateAndUploadTts(testText, dummyBookId, dummyPageNumber);
        return ResponseEntity.ok(audioUrl);
    }
}