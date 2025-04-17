//package hello.booktown.controller;
//
//import hello.booktown.dto.BookResponse;
//import hello.booktown.dto.GutendexRequest;
//import hello.booktown.dto.SummarySceneResponse;
//import hello.booktown.service.BookService;
//import hello.booktown.service.StabilityAIService;
//import hello.booktown.service.SummaryService;
//import hello.booktown.util.CustomUserDetails;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.IOException;
//import java.util.List;
//
//@RestController
//@RequestMapping("/test")
//@Tag(name = "TestController", description = "Test endpoint for verifying deployment")
//public class TestController {
//
//    private final ChatClient chatClient;
//    private StabilityAIService stabilityAIService;
//    private final BookService bookService;
//    private final SummaryService summaryService;
//
//    public TestController(ChatClient.Builder chatClientBuilder, StabilityAIService stabilityAIService, BookService bookService, SummaryService summaryService) {
//        this.chatClient = chatClientBuilder.build();
//        this.stabilityAIService = stabilityAIService;
//        this.bookService = bookService;
//        this.summaryService = summaryService;
//    }
//
//    // 기존 AI 생성 요청 처리
//    @GetMapping("/ai")
//    public String generation(@RequestParam String userInput) {
//        return this.chatClient.prompt()
//                .user(userInput)
//                .call()
//                .content();
//    }
//
////    @GetMapping("/generate-image")
////    public String generateImage(@RequestParam String prompt) {
////        return stabilityAIService.generateThumbnail(prompt);
////    }
//
//
//    // 테스트 엔드포인트 (배포 확인용)
//    @Operation(summary = "테스트 엔드포인트", description = "서버가 정상적으로 배포되었는지 확인하는 엔드포인트입니다.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "성공적으로 처리됨")
//    })
//    @GetMapping("/qwe")
//    public ResponseEntity<String> testEndpoint(HttpServletRequest request, HttpServletResponse response) {
//        System.out.println("테스트!!");
//
//        return new ResponseEntity<>("OK", HttpStatus.OK);
//    }
//
//    // 책 등록
//    @PostMapping("/register")
//    public ResponseEntity<String> registerBook(@RequestBody GutendexRequest request) {
//        bookService.saveBookFromGutenberg(request.getGutenbergId());
//        return ResponseEntity.ok("✅ 성공적으로 책이 등록되었습니다.");
//    }
//
//    @GetMapping("/{bookId}")
//    public ResponseEntity<BookResponse> getBook(@PathVariable Long bookId) {
//        BookResponse response = bookService.getBookById(bookId);
//        return ResponseEntity.ok(response);
//    }
//
//
//    @Operation(summary = "책 요약 생성 및 반환", description = "책 전체를 요약하고 10개의 씬과 그림을 반환합니다.")
//    @PostMapping
//    public ResponseEntity<List<SummarySceneResponse>> summarizeAndGetSummary(
//            @RequestParam Long bookId,
//            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {
//
//        Long userId = userDetails.getUserId();
//        List<SummarySceneResponse> summaryScenes = summaryService.getSummaryScenes(userId, bookId);
//        return ResponseEntity.ok(summaryScenes);
//    }
//}
