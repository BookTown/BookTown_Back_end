package hello.booktown.controller;

import hello.booktown.domain.Book;
import hello.booktown.dto.BookResponse;
import hello.booktown.dto.GutendexRequest;
import hello.booktown.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/book")
public class BookController {

    private final BookService bookService;

    @Operation(summary = "책 랜덤 조회", description = "메인 화면에 위치하는 랜덤 책을 조회합니다.")
    @GetMapping("/banner")
    public ResponseEntity<Book> getRandomBook() {
        return ResponseEntity.ok(bookService.getRandomBook());
    }

    @Operation(summary = "좋아요 순으로 책 조회", description = "좋아요 수를 기준으로 책을 조회합니다.")
    @GetMapping("/popular")
    public ResponseEntity<List<Book>> getTopLikedBooks() {
        return ResponseEntity.ok(bookService.getTopLikedBooks());
    }

    @Operation(summary = "좋아요 순으로 모든 책 조회", description = "좋아요 수를 기준으로 모든 책을 조회합니다.")
    @GetMapping("/popular/all")
    public ResponseEntity<List<Book>> getAllBooksByLikes() {
        return ResponseEntity.ok(bookService.getAllBooksByLikes());
    }

    @Operation(summary = "최근 등록된 책 조회", description = "최근에 등록된 책을 조회합니다.")
    @GetMapping("/recent")
    public ResponseEntity<List<Book>> getLatestBooks() {
        return ResponseEntity.ok(bookService.getLatestBooks());
    }

    @Operation(summary = "최근 등록된 모든 책 조회", description = "최근에 등록된 모든 책을 조회합니다.")
    @GetMapping("/recent/all")
    public ResponseEntity<List<Book>> getAllBooksByCreatedAt() {
        return ResponseEntity.ok(bookService.getAllBooksByCreatedAt());
    }

    @Operation(
            summary = "책 정보 조회",
            description = "책의 줄거리 보기나 퀴즈 풀기 선택지를 제공하는 책 정보 조회."
    )
    @GetMapping("/info")
    public ResponseEntity<BookResponse> getBookInfo(@RequestParam Long bookId) {
        BookResponse response = bookService.getBookInfo(bookId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "책 등록 하기", description = "Gutendex API를 사용해 책의 정보를 불러와 DB에 저장합니다.")
    @PostMapping("/register")
    public ResponseEntity<String> registerBook(@RequestBody GutendexRequest request) {
        bookService.saveBookFromGutenberg(request.getGutenbergId());
        return ResponseEntity.ok("✅ 성공적으로 책이 등록되었습니다.");
    }

}
