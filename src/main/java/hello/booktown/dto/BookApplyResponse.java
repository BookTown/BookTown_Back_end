package hello.booktown.dto;

import hello.booktown.domain.enums.ApplyStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookApplyResponse {
    private Long id;
    private String title;
    private ApplyStatus status;
    private String appliedDate; // 날짜만 출력
    private String rejectionReason;
}