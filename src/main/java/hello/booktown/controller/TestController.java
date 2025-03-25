package hello.booktown.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "TestController", description = "Test endpoint for verifying deployment")
public class TestController {

    @Operation(summary = "테스트 엔드포인트", description = "서버가 정상적으로 배포되었는지 확인하는 엔드포인트입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 처리됨")
    })
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("테스트.");

        return new ResponseEntity<>("OK", HttpStatus.OK);
    }
}
