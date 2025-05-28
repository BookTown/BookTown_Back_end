package hello.booktown.controller;

import hello.booktown.dto.BookApplyRejectRequest;
import hello.booktown.dto.BookApplyRequest;
import hello.booktown.dto.BookApplyResponse;
import hello.booktown.service.BookApplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apply")
@RequiredArgsConstructor
@Tag(name = "Book Apply", description = "책 신청 관련 API")
public class BookApplyController {

    private final BookApplyService bookApplyService;

    @PostMapping
    @Operation(summary = "책 신청", description = "사용자가 책 제목으로 신청합니다.")
    public ResponseEntity<BookApplyResponse> applyBook(@RequestBody BookApplyRequest request) {
        BookApplyResponse response = bookApplyService.applyBook(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "책 신청 거부", description = "관리자가 책 신청을 거부하고 사유를 입력합니다.")
    public ResponseEntity<BookApplyResponse> rejectBookApply(
            @PathVariable Long id,
            @RequestBody BookApplyRejectRequest request) {
        BookApplyResponse response = bookApplyService.rejectBookApply(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "책 신청 승인", description = "관리자가 책 신청을 승인합니다.")
    public ResponseEntity<BookApplyResponse> approveBookApply(@PathVariable Long id) {
        BookApplyResponse response = bookApplyService.approveBookApply(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    @Operation(summary = "전체 신청 조회", description = "모든 책 신청 내역을 조회합니다.")
    public ResponseEntity<List<BookApplyResponse>> getAllBookApplies() {
        List<BookApplyResponse> responses = bookApplyService.getAllBookApplies();
        return ResponseEntity.ok(responses);
    }
}