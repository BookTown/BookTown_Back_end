package hello.booktown.controller;

import hello.booktown.dto.RankResponseDto;
import hello.booktown.service.RankService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @Operation(summary = "Top 3 랭킹 조회", description = "점수 기준으로 1~3등 유저 반환")
    @GetMapping("/top3")
    public ResponseEntity<List<RankResponseDto>> getTop3Rank() {
        return ResponseEntity.ok(rankService.getTop3Rank());
    }

    @Operation(summary = "전체 유저 랭킹 조회", description = "모든 유저를 점수 내림차순으로 정렬하여 반환")
    @GetMapping("/all")
    public ResponseEntity<List<RankResponseDto>> getAllRank() {
        return ResponseEntity.ok(rankService.getAllRank());
    }
}
