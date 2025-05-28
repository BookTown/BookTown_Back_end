package hello.booktown.service;

import hello.booktown.domain.BookApply;
import hello.booktown.domain.enums.ApplyStatus;
import hello.booktown.dto.BookApplyRequest;
import hello.booktown.dto.BookApplyResponse;
import hello.booktown.repository.BookApplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookApplyService {

    private final BookApplyRepository bookApplyRepository;

    public BookApplyResponse applyBook(BookApplyRequest request) {
        BookApply apply = BookApply.builder()
                .title(request.getTitle())
                .status(ApplyStatus.PENDING)
                .appliedAt(LocalDateTime.now())
                .build();

        BookApply saved = bookApplyRepository.save(apply);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        return BookApplyResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .status(saved.getStatus())
                .appliedDate(saved.getAppliedAt().format(formatter))  // 포맷 적용
                .rejectionReason(saved.getRejectionReason())
                .build();
    }
    public BookApplyResponse rejectBookApply(Long id, hello.booktown.dto.BookApplyRejectRequest request) {
        BookApply apply = bookApplyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 신청이 존재하지 않습니다."));
        apply.setStatus(ApplyStatus.REJECTED);
        apply.setRejectionReason(request.getReason());
        bookApplyRepository.save(apply);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        return BookApplyResponse.builder()
                .id(apply.getId())
                .title(apply.getTitle())
                .status(apply.getStatus())
                .appliedDate(apply.getAppliedAt().format(formatter))
                .rejectionReason(apply.getRejectionReason())
                .build();
    }

    public BookApplyResponse approveBookApply(Long id) {
        BookApply apply = bookApplyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 신청이 존재하지 않습니다."));
        apply.setStatus(ApplyStatus.APPROVED);
        apply.setRejectionReason(null);
        bookApplyRepository.save(apply);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        return BookApplyResponse.builder()
                .id(apply.getId())
                .title(apply.getTitle())
                .status(apply.getStatus())
                .appliedDate(apply.getAppliedAt().format(formatter))
                .rejectionReason(apply.getRejectionReason())
                .build();
    }

    public List<BookApplyResponse> getAllBookApplies() {
        return bookApplyRepository.findAll().stream()
                .map(apply -> BookApplyResponse.builder()
                        .id(apply.getId())
                        .title(apply.getTitle())
                        .status(apply.getStatus())
                        .appliedDate(apply.getAppliedAt().toLocalDate().toString())
                        .rejectionReason(apply.getRejectionReason())
                        .build())
                .toList();
    }
}