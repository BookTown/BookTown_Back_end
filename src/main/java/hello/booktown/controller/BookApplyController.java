package hello.booktown.controller;

import hello.booktown.dto.BookApplyRejectRequest;
import hello.booktown.dto.BookApplyRequest;
import hello.booktown.dto.BookApplyResponse;
import hello.booktown.service.BookApplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apply")
@RequiredArgsConstructor
public class BookApplyController {

    private final BookApplyService bookApplyService;

    @PostMapping
    public ResponseEntity<BookApplyResponse> applyBook(@RequestBody BookApplyRequest request) {
        BookApplyResponse response = bookApplyService.applyBook(request);
        return ResponseEntity.ok(response);
    }
    @PatchMapping("/{id}/reject")
    public ResponseEntity<BookApplyResponse> rejectBookApply(
            @PathVariable Long id,
            @RequestBody BookApplyRejectRequest request) {
        BookApplyResponse response = bookApplyService.rejectBookApply(id, request);
        return ResponseEntity.ok(response);
    }
    @PatchMapping("/{id}/approve")
    public ResponseEntity<BookApplyResponse> approveBookApply(@PathVariable Long id) {
        BookApplyResponse response = bookApplyService.approveBookApply(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<BookApplyResponse>> getAllBookApplies() {
        List<BookApplyResponse> responses = bookApplyService.getAllBookApplies();
        return ResponseEntity.ok(responses);
    }
}