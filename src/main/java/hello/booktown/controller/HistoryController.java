package hello.booktown.controller;

import hello.booktown.dto.HistoryDetailResponseDto;
import hello.booktown.dto.HistoryResponseDto;
import hello.booktown.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @Operation(summary = "전체 퀴즈 히스토리 조회", description = "사용자 전체 퀴즈 히스토리를 조회합니다. 퀴즈 결과와 책 정보를 포함합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<List<HistoryResponseDto>> getAllHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(historyService.getAllHistories(userId));
    }

    @Operation(summary = "특정 책에 대한 퀴즈 히스토리 상세 조회", description = "해당 책에 대한 퀴즈 10개 제출 결과를 반환합니다.")
    @GetMapping("/{userId}/book/{bookId}/{groupIndex}")
    public ResponseEntity<HistoryDetailResponseDto> getQuizHistory(
            @PathVariable Long userId,
            @PathVariable Long bookId,
            @PathVariable int groupIndex) {
        return ResponseEntity.ok(historyService.getHistoryDetail(userId, bookId, groupIndex));
    }


    @Operation(summary = "퀴즈 히스토리 그룹 삭제", description = "특정 책과 그룹 인덱스에 해당하는 퀴즈 기록을 삭제합니다.")
    @DeleteMapping("/{userId}/book/{bookId}/{groupIndex}")
    public ResponseEntity<String> deleteQuizHistory(
            @PathVariable Long userId,
            @PathVariable Long bookId,
            @PathVariable int groupIndex) {
        historyService.deleteHistory(userId, bookId, groupIndex);
        return ResponseEntity.ok("해당 퀴즈 히스토리 그룹이 삭제되었습니다.");
    }

}
