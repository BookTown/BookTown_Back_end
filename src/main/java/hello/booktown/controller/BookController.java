package hello.booktown.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/book")
public class BookController {

    @Operation(summary = "책 랜덤 조회", description = "메인 화면에 위치하는 랜덤 책을 조회합니다.")
    @GetMapping("/banner")
    public void getRandomBook() {

    }

    @Operation(summary = "좋아요 순으로 책 조회", description = "좋아요 수를 기준으로 책을 조회합니다.")
    @GetMapping("/popular")
    public void getPopularBooks() {

    }

    @Operation(summary = "좋아요 순으로 모든 책 조회", description = "좋아요 수를 기준으로 모든 책을 조회합니다.")
    @GetMapping("/popular/all")
    public void getAllPopularBooks() {

    }

    @Operation(summary = "최근 등록된 책 조회", description = "최근에 등록된 책을 조회합니다.")
    @GetMapping("/recent")
    public void getRecentBooks() {

    }

    @Operation(summary = "최근 등록된 모든 책 조회", description = "최근에 등록된 모든 책을 조회합니다.")
    @GetMapping("/recent/all")
    public void getAllRecentBooks() {

    }

    @Operation(summary = "책 정보 조회", description = "책의 줄거리 보기나 퀴즈 풀기 선택지를 제공하는 책 정보 조회.")
    @GetMapping("/info")
    public void getBookInfo() {

    }
}
