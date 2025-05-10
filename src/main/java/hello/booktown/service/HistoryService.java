// HistoryService.java (History 엔티티 없이 QuizSubmission 기반)
package hello.booktown.service;

import hello.booktown.domain.Book;
import hello.booktown.domain.QuizSubmission;
import hello.booktown.domain.User;
import hello.booktown.dto.HistoryResponseDto;
import hello.booktown.dto.HistoryDetailResponseDto;
import hello.booktown.dto.QuizSubmissionHistoryDto;
import hello.booktown.exception.CustomException;
import hello.booktown.exception.ErrorCode;
import hello.booktown.repository.QuizSubmissionRepository;
import hello.booktown.repository.UserRepository;
import hello.booktown.repository.QuizRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final QuizSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;

    public List<HistoryResponseDto> getAllHistories(Long userId) {
        validateUser(userId);
        List<QuizSubmission> submissions = submissionRepository.findByUserId(userId);

        Map<Book, List<QuizSubmission>> grouped = submissions.stream()
                .collect(Collectors.groupingBy(s -> s.getQuiz().getBookSummary().getBook()));

        return grouped.entrySet().stream().map(entry -> {
            Book book = entry.getKey();
            List<QuizSubmission> subs = entry.getValue();
            int score = subs.stream().filter(QuizSubmission::isCorrect).mapToInt(s -> s.getQuiz().getScore()).sum();
            String submittedAt = subs.stream().map(s -> {
                if (s.getSubmittedAt() != null) return s.getSubmittedAt().toString();
                else return "";
            }).findFirst().orElse("");
            return new HistoryResponseDto(null, book.getId(), book.getTitle(), score, submittedAt);
        }).toList();
    }

    public HistoryDetailResponseDto getHistoryDetail(Long userId, Long bookId) {
        validateUser(userId);
        List<QuizSubmission> all = submissionRepository.findByUserId(userId);

        List<QuizSubmission> filtered = all.stream()
                .filter(s -> s.getQuiz().getBookSummary().getBook().getId().equals(bookId))
                .toList();

        if (filtered.isEmpty()) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        Book book = filtered.get(0).getQuiz().getBookSummary().getBook();
        int totalScore = filtered.stream().filter(QuizSubmission::isCorrect).mapToInt(s -> s.getQuiz().getScore()).sum();
        String submittedAt = filtered.get(0).getSubmittedAt() != null ? filtered.get(0).getSubmittedAt().toString() : "";

        return HistoryDetailResponseDto.builder()
                .historyId(null)
                .bookTitle(book.getTitle())
                .totalScore(totalScore)
                .submittedAt(submittedAt)
                .submissions(filtered.stream().map(QuizSubmission::toHistoryDto).toList())
                .build();
    }

    @Transactional
    public void deleteHistoryByBook(Long userId, Long bookId) {
        validateUser(userId);
        List<QuizSubmission> submissions = submissionRepository.findByUserId(userId).stream()
                .filter(s -> s.getQuiz().getBookSummary().getBook().getId().equals(bookId))
                .toList();

        if (!submissions.isEmpty()) {
            submissionRepository.deleteAll(submissions);
            quizRepository.deleteByUserAndBook(userRepository.findById(userId).orElseThrow(), submissions.get(0).getQuiz().getBookSummary().getBook());
        }
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
