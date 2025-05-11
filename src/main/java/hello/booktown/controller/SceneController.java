package hello.booktown.controller;

import hello.booktown.service.StabilityAIService;
import org.springframework.core.io.Resource;
import hello.booktown.dto.SummaryRequest;
import hello.booktown.dto.SummarySceneResponse;
import hello.booktown.service.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/summaries")
@RequiredArgsConstructor
@Slf4j
public class SceneController {
    @Value("classpath:/prompts/scene-prompt.st") // 테스트용
    private Resource scenePromptResource; //테스트용
    private final StabilityAIService stabilityAIService; //테스트용
    private final SummaryService summaryService;

    @Operation(summary = "책 요약 생성 및 반환", description = "책 전체를 요약하고 10개의 씬과 그림을 반환합니다. 이미 요약된 경우 기존 결과를 반환합니다.")
    @PostMapping
    public ResponseEntity<List<SummarySceneResponse>> summarizeAndGetSummary(@RequestBody SummaryRequest summaryRequest) throws IOException {
        Long bookId = summaryRequest.getBookId();

        // 책 줄거리 및 씬 생성 (이미 있으면 생략)
        summaryService.summarizeBook(bookId);

        // 요약된 씬 목록 반환
        List<SummarySceneResponse> summaryScenes = summaryService.getSummaryScenes(bookId);
        return ResponseEntity.ok(summaryScenes);
    }

    @Operation(summary = "요약된 씬 조회", description = "특정 책에 대한 요약된 씬 목록을 조회합니다.")
    @PostMapping("/lookup")
    public ResponseEntity<List<SummarySceneResponse>> lookupSummaryScenes(@RequestBody SummaryRequest summaryRequest) {
        Long bookId = summaryRequest.getBookId();

        List<SummarySceneResponse> summaryScenes = summaryService.getSummaryScenes(bookId);
        return ResponseEntity.ok(summaryScenes);
    }


}
