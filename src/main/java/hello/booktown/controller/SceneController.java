package hello.booktown.controller;

import hello.booktown.dto.SummarySceneResponse;
import hello.booktown.exception.CustomException;
import hello.booktown.exception.ErrorCode;
import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.service.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/summaries")
@RequiredArgsConstructor
@Slf4j
public class SceneController {

    private final SummaryService summaryService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "책 요약 생성 및 반환", description = "책 전체를 요약하고 10개의 씬과 그림을 반환합니다. 이미 요약된 경우 기존 결과를 반환합니다.")
    @PostMapping
    public ResponseEntity<List<SummarySceneResponse>> summarizeAndGetSummary(
            @RequestParam Long bookId,
            HttpServletRequest request) throws IOException {


        // JWT 토큰에서 userId 추출
        String token = jwtTokenProvider.resolveToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);  // 유효하지 않은 토큰에 대한 예외 처리
        }

        // 토큰에서 userId 추출
        String userId = jwtTokenProvider.getUsernameFromToken(token);
        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);  // 유효하지 않은 userId 처리
        }
        System.out.println(userId + " " + bookId);
        summaryService.summarizeBookForUser(Long.parseLong(userId), bookId);

        // 최종 요약 결과 반환
        List<SummarySceneResponse> summaryScenes = summaryService.getSummaryScenes(Long.parseLong(userId), bookId);
        return ResponseEntity.ok(summaryScenes);
    }



}

