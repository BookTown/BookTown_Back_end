package hello.booktown.controller;

import hello.booktown.service.StabilityAIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@Tag(name = "TestController", description = "Test endpoint for verifying deployment")
public class TestController {

    private final ChatClient chatClient;
    private StabilityAIService stabilityAIService;

    public TestController(ChatClient.Builder chatClientBuilder, StabilityAIService stabilityAIService) {
        this.chatClient = chatClientBuilder.build();
        this.stabilityAIService = stabilityAIService;
    }

    // 기존 AI 생성 요청 처리
    @GetMapping("/ai")
    public String generation(@RequestParam String userInput) {
        return this.chatClient.prompt()
                .user(userInput)
                .call()
                .content();
    }

    @GetMapping("/generate-image")
    public String generateImage(@RequestParam String prompt) {
        return stabilityAIService.generateImage(prompt);
    }


    // 테스트 엔드포인트 (배포 확인용)
    @Operation(summary = "테스트 엔드포인트", description = "서버가 정상적으로 배포되었는지 확인하는 엔드포인트입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 처리됨")
    })
    @GetMapping("/qwe")
    public ResponseEntity<String> testEndpoint(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("테스트!!");

        return new ResponseEntity<>("OK", HttpStatus.OK);
    }
}
