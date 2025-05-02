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

    @GetMapping("/test/scene-image")
    public ResponseEntity<String> generateExampleSceneImage() throws IOException {
        // 1. 예시 줄거리 (영문 번역된 요약)
        String exampleScene = "At the end of summer, Nick Carraway leaves his Midwestern hometown and gazes out the train window as the scenery shifts. " +
                "As the train speeds toward New York, the familiar landscape becomes more urban, stirring a sense of excitement within him. " +
                "Remembering his father's advice—'Don’t judge others'—Nick reflects quietly. " +
                "Though his family prided themselves on Midwestern tradition and wealth, Nick is determined to start a new life in the East. " +
                "His heart is filled with hope for a new job and future in New York, unaware that the world awaiting him will be nothing like he imagined.";

        // 2. 프롬프트 템플릿 불러오기
        String template = new String(scenePromptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        // 3. {{scene}} 치환
        String finalPrompt = template.replace("{{scene}}", exampleScene);

        // 4. 이미지 생성 요청 (userId/bookId는 임의 값)
        String imageUrl = stabilityAIService.generateSceneImage(finalPrompt, 999L, 1);

        return ResponseEntity.ok(imageUrl);
    }
//    @GetMapping("/test/scene-image")
//    public ResponseEntity<List<String>> generateMultiSceneImagesWithContext() throws IOException {
//        List<String> exampleScenes = List.of(
//                "Scene 1: After the storm, the boy wanders through the ruins, searching for something that might have survived.",
//                "Scene 2: In the rubble, he discovers the last remaining compass, partially buried in the dirt.",
//                "Scene 3: As he holds it in his hands, the sky turns red and a rainbow appears, filling the scene with quiet hope."
//        );
//
//        String commonIntro = "These scenes are part of one continuous story about a boy’s journey through a ruined land after a storm. Each illustration must follow the same characters, tone, and world.";
//        List<String> imageUrls = new ArrayList<>();
//        String template = new String(scenePromptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
//
//        for (int i = 0; i < exampleScenes.size(); i++) {
//            String sceneDescription = commonIntro + "\n\n" + exampleScenes.get(i);
//            String finalPrompt = template.replace("{{scene}}", sceneDescription);
//            String imageUrl = stabilityAIService.generateSceneImage(finalPrompt, 999L, i + 1);
//            imageUrls.add(imageUrl);
//        }
//
//        return ResponseEntity.ok(imageUrls);
//    }
}
