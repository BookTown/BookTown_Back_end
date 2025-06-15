package hello.booktown.controller;

import com.google.cloud.texttospeech.v1.SsmlVoiceGender;

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "TTS API", description = "Text-to-Speech 관련 API입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tts")
public class TtsController {

    private final TtsService ttsService;
    private final BookSummaryRepository bookSummaryRepository;
    private final SummarySceneRepository summarySceneRepository;

    @Operation(summary = "기존 summaryScene으로부터 TTS 재생성", description = "기존 bookId로 저장된 summaryScene을 기반으로 FEMALE/MALE TTS를 재생성하고 DB에 반영합니다.")
    @PostMapping("/regenerate/{bookId}")
    public ResponseEntity<String> regenerateTtsForScenes(@PathVariable Long bookId) {
        BookSummary summary = bookSummaryRepository.findByBookId(bookId)
                .orElseThrow(() -> new IllegalArgumentException("해당 책 요약이 존재하지 않습니다."));

        List<SummaryScene> scenes = summarySceneRepository.findByBookSummary(summary);

        for (SummaryScene scene : scenes) {
            int page = scene.getPageNumber();
            String content = scene.getContent();

            String femaleUrl = ttsService.generateAndUploadTts(content, bookId, page, SsmlVoiceGender.FEMALE);
            String maleUrl = ttsService.generateAndUploadTts(content, bookId, page, SsmlVoiceGender.MALE);

            scene.setFemaleAudioUrl(femaleUrl);
            scene.setMaleAudioUrl(maleUrl);
        }

        summarySceneRepository.saveAll(scenes);
        return ResponseEntity.ok("TTS 재생성 및 DB 업데이트 완료");
    }


    @Operation(summary = "요약의 전체 오디오 URL 조회", description = "summaryId에 해당하는 모든 장면의 오디오 URL 목록을 반환합니다.")
    @GetMapping("/summary/{summaryId}/audio")
    public ResponseEntity<List<String>> getAllAudioUrls(
            @PathVariable Long summaryId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "FEMALE") SsmlVoiceGender gender) {
        List<SummaryScene> scenes = summarySceneRepository.findByBookSummaryIdOrderByPageNumberAsc(summaryId);
        List<String> audioUrls = scenes.stream()
                .map(scene -> gender == SsmlVoiceGender.MALE ? scene.getMaleAudioUrl() : scene.getFemaleAudioUrl())
                .toList();
        return ResponseEntity.ok(audioUrls);
    }

    @Operation(summary = "특정 장면의 오디오 URL 조회", description = "summaryId와 pageNumber에 해당하는 오디오 URL을 반환합니다.")
    @GetMapping("/summary/{summaryId}/audio/{pageNumber}")
    public ResponseEntity<String> getAudioUrlByPage(
            @PathVariable Long summaryId,
            @PathVariable int pageNumber,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "FEMALE") SsmlVoiceGender gender) {

        SummaryScene scene = summarySceneRepository.findByBookSummaryIdAndPageNumber(summaryId, pageNumber);
        if (scene == null) return ResponseEntity.notFound().build();

        String url = gender == SsmlVoiceGender.MALE ? scene.getMaleAudioUrl() : scene.getFemaleAudioUrl();
        return url != null ? ResponseEntity.ok(url) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "TTS 테스트용 오디오 생성", description = "기본 텍스트로 TTS 오디오를 생성하고 S3에 업로드하여 URL을 반환합니다.")
    @PostMapping("/test")
    public ResponseEntity<String> testTtsGeneration() {
        String testText = "안녕하세요. 북타운에 오신 것을 환영합니다.";
        Long dummyBookId = 999L; // 테스트용 bookId (실제 저장에는 사용되지 않아도 됨)
        int dummyPageNumber = 0;

        String audioUrl = ttsService.generateAndUploadTts(testText, dummyBookId, dummyPageNumber, SsmlVoiceGender.FEMALE);
        return ResponseEntity.ok(audioUrl);
    }
}