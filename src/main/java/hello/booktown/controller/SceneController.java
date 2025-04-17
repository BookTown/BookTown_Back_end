package hello.booktown.controller;

import hello.booktown.dto.BookSummaryResponse;
import hello.booktown.dto.SummaryRequest;
import hello.booktown.dto.SummarySceneResponse;
import hello.booktown.service.SummaryService;
import hello.booktown.util.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/summaries")
@RequiredArgsConstructor
public class SceneController {

    private final SummaryService summaryService;

    @Operation(summary = "책 요약 생성 및 반환", description = "책 전체를 요약하고 10개의 씬과 그림을 반환합니다.")
    @PostMapping
    public ResponseEntity<List<SummarySceneResponse>> summarizeAndGetSummary(
            @RequestParam Long bookId,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        Long userId = userDetails.getUserId();
        List<SummarySceneResponse> summaryScenes = summaryService.getSummaryScenes(userId, bookId);
        return ResponseEntity.ok(summaryScenes);
    }

}

