package hello.booktown.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    @Operation(summary = "사용자 프로필 조회", description = "사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/{userId}")
    public void getProfile() {

    }

    @Operation(summary = "사용자 프로필 업데이트", description = "사용자의 프로필 정보를 업데이트합니다.")
    @PatchMapping("/{userId}")
    public void updateProfile() {

    }

    @Operation(summary = "사용자가 관심을 가진 책 조회", description = "사용자가 관심을 가진 책 리스트를 조회합니다.")
    @GetMapping("/{userId}/liked-books")
    public void getLikedBooks() {

    }

    @Operation(summary = "사용자가 퀴즈 난이도 설정", description = "사용자가 퀴즈 난이도를 설정합니다.")
    @PatchMapping("/{userId}/difficulty")
    public void setDifficulty() {

    }

    @Operation(summary = "사용자가 원하는 고전 신청", description = "사용자가 원하는 고전을 신청합니다.")
    @PostMapping("/{userId}/requested-books")
    public void requestBook() {

    }

    @Operation(summary = "사용자 회원탈퇴", description = "사용자가 회원탈퇴를 합니다.")
    @DeleteMapping("/{userId}")
    public void deleteProfile() {

    }
}
