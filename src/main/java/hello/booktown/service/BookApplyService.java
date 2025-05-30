package hello.booktown.service;

import hello.booktown.domain.BookApply;
import hello.booktown.domain.enums.ApplyStatus;
import hello.booktown.dto.BookApplyRequest;
import hello.booktown.dto.BookApplyResponse;
import hello.booktown.repository.BookApplyRepository;
import hello.booktown.domain.User;
import hello.booktown.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookApplyService {

    private final BookApplyRepository bookApplyRepository;
    private final UserRepository userRepository;

    public BookApplyResponse applyBook(BookApplyRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        BookApply apply = BookApply.builder()
                .title(request.getTitle())
                .user(user)
                .status(ApplyStatus.PENDING)
                .appliedAt(LocalDateTime.now())
                .build();

        BookApply saved = bookApplyRepository.save(apply);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        return BookApplyResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .status(saved.getStatus())
                .appliedDate(saved.getAppliedAt().format(formatter))
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
                        .appliedDate(apply.getAppliedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                        .rejectionReason(apply.getRejectionReason())
                        .build())
                .toList();
    }

    @Transactional
    public void deleteBookApply(Long id) {
        if (!bookApplyRepository.existsById(id)) {
            throw new RuntimeException("존재하지 않는 ID입니다.");
        }
        bookApplyRepository.deleteById(id);
        bookApplyRepository.flush(); // 바로 DB에 반영
    }

    public List<BookApplyResponse> getBookAppliesByUser(Long userId) {
        return bookApplyRepository.findByUserIdOrderByAppliedAtDesc(userId).stream()
                .map(apply -> BookApplyResponse.builder()
                        .id(apply.getId())
                        .title(apply.getTitle())
                        .status(apply.getStatus())
                        .appliedDate(apply.getAppliedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                        .rejectionReason(apply.getRejectionReason())
                        .build())
                .toList();
    }
}