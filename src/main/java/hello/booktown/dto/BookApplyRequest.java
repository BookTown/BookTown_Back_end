package hello.booktown.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookApplyRequest {
    private String title;  // 신청 시 입력받을 책 제목
}