package hello.booktown.service;

import hello.booktown.domain.Book;
import hello.booktown.domain.QuizSubmission;
import hello.booktown.domain.QuizSubmissionGroup;
import hello.booktown.dto.HistoryDetailResponseDto;
import hello.booktown.dto.HistoryResponseDto;
import hello.booktown.exception.CustomException;
import hello.booktown.exception.ErrorCode;
import hello.booktown.repository.QuizSubmissionGroupRepository;
import hello.booktown.repository.QuizSubmissionRepository;
import hello.booktown.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final QuizSubmissionRepository submissionRepository;
    private final QuizSubmissionGroupRepository groupRepository;
    private final UserRepository userRepository;

    public List<HistoryResponseDto> getAllHistories(Long userId) {
        validateUser(userId);

        // 해당 유저의 모든 퀴즈 제출 그룹 조회
        List<QuizSubmissionGroup> groups = groupRepository.findByUser_Id(userId);

        return groups.stream().map(group -> {
            Book book = group.getBook();
            List<QuizSubmission> subs = group.getSubmissions();

            int score = subs.stream()
                    .filter(QuizSubmission::isCorrect)
                    .mapToInt(s -> s.getQuiz().getScore())
                    .sum();

            return new HistoryResponseDto(
                    group.getId(),
                    book.getId(),
                    book.getTitle(),
                    score,
                    group.getSubmittedAt() != null ? group.getSubmittedAt().toString() : "",
                    group.getGroupIndex()
            );
        }).toList();
    }


    public HistoryDetailResponseDto getHistoryDetail(Long userId, Long bookId, int index) {
        QuizSubmissionGroup group = groupRepository.findByUserIdAndBookIdAndGroupIndex(userId, bookId, index)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST));

        List<QuizSubmission> submissions = group.getSubmissions();

        int totalScore = submissions.stream()
                .filter(QuizSubmission::isCorrect)
                .mapToInt(s -> s.getQuiz().getScore())
                .sum();

        return HistoryDetailResponseDto.builder()
                .historyId(group.getId())
                .bookTitle(group.getBook().getTitle())
                .totalScore(totalScore)
                .submittedAt(group.getSubmittedAt().toString())
                .submissions(submissions.stream().map(QuizSubmission::toHistoryDto).toList())
                .build();
    }


    @Transactional
    public void deleteHistory(Long userId, Long bookId, int groupIndex) {
        validateUser(userId);
        QuizSubmissionGroup group = groupRepository.findByUserIdAndBookIdAndGroupIndex(userId, bookId, groupIndex)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST));

        groupRepository.delete(group); // cascade = ALL이면 submissions도 삭제됨
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
