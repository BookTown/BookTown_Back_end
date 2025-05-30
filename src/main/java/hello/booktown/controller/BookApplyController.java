package hello.booktown.controller;

import hello.booktown.domain.BookApply;
import hello.booktown.dto.BookApplyRejectRequest;
import hello.booktown.dto.BookApplyRequest;
import hello.booktown.dto.BookApplyResponse;
import hello.booktown.exception.CustomException;
import hello.booktown.exception.ErrorCode;
import hello.booktown.jwt.JwtTokenProvider;
import hello.booktown.repository.BookApplyRepository;
import hello.booktown.service.BookApplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final BookApplyRepository bookApplyRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private Long extractUserId(HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        return Long.parseLong(jwtTokenProvider.getUsernameFromToken(token));
    }

    @PostMapping
    @Operation(summary = "책 신청", description = "사용자가 책 제목으로 신청합니다.")
    public ResponseEntity<BookApplyResponse> applyBook(
            @RequestBody BookApplyRequest request,
            HttpServletRequest requestContext) {

        Long currentUserId = extractUserId(requestContext);
        BookApplyResponse response = bookApplyService.applyBook(request, currentUserId);
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

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "책 신청 삭제", description = "본인이 작성한 책 신청을 삭제합니다.")
    public ResponseEntity<String> deleteBookApply(@PathVariable Long id, HttpServletRequest requestContext) {
        Long currentUserId = extractUserId(requestContext);

        BookApply apply = bookApplyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 신청이 존재하지 않습니다."));

        if (!apply.getUser().getId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        bookApplyRepository.deleteById(id);
        return ResponseEntity.ok("삭제 완료");
    }

    @GetMapping("/user")
    @Operation(summary = "내 책 신청 조회", description = "사용자가 자신이 신청한 책 목록을 조회합니다.")
    public ResponseEntity<List<BookApplyResponse>> getMyApplies(HttpServletRequest requestContext) {
        Long currentUserId = extractUserId(requestContext);
        return ResponseEntity.ok(bookApplyService.getBookAppliesByUser(currentUserId));
    }
}