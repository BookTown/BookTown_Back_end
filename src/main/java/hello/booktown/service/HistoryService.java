package hello.booktown.service;

import hello.booktown.domain.Book;
import hello.booktown.domain.QuizSubmission;
import hello.booktown.domain.QuizSubmissionGroup;
import hello.booktown.dto.GroupedHistoryDto;
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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final QuizSubmissionRepository submissionRepository;
    private final QuizSubmissionGroupRepository groupRepository;
    private final UserRepository userRepository;

    public List<GroupedHistoryDto> getAllGroupedHistories(Long userId) {
        validateUser(userId);

        List<QuizSubmissionGroup> groups = groupRepository.findByUser_Id(userId);

        // 그룹핑: 책 기준
        Map<Book, List<QuizSubmissionGroup>> bookToGroups = groups.stream()
                .collect(Collectors.groupingBy(QuizSubmissionGroup::getBook));

        return bookToGroups.entrySet().stream().map(entry -> {
            Book book = entry.getKey();
            List<QuizSubmissionGroup> groupList = entry.getValue();

            List<HistoryResponseDto> histories = groupList.stream()
                    .sorted(Comparator.comparingInt(QuizSubmissionGroup::getGroupIndex))
                    .map(group -> {
                        int score = group.getSubmissions().stream()
                                .filter(QuizSubmission::isCorrect)
                                .mapToInt(s -> s.getQuiz().getScore())
                                .sum();

                        return HistoryResponseDto.builder()
                                .id(group.getId())
                                .bookId(book.getId())
                                .bookTitle(book.getTitle())
                                .score(score)
                                .submittedAt(group.getSubmittedAt() != null ? group.getSubmittedAt().toString() : "")
                                .groupIndex(group.getGroupIndex())
                                .build();
                    }).toList();

            return GroupedHistoryDto.builder()
                    .bookId(book.getId())
                    .title(book.getTitle())
                    .author(book.getAuthor())
                    .histories(histories)
                    .build();
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
