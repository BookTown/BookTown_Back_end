package hello.booktown.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rank")
public class RankController {

    @Operation(summary = "랭킹 1~100등 조회", description = "1등부터 100등까지의 랭킹을 조회합니다.")
    @GetMapping("/top100")
    public void getTop100Rank() {
        // 구현 내용
    }

    @Operation(summary = "1등, 2등, 3등 랭킹 조회", description = "1등, 2등, 3등의 랭킹 정보를 조회합니다.")
    @GetMapping("/top3")
    public void getTop3Rank() {
        // 구현 내용
    }
}
