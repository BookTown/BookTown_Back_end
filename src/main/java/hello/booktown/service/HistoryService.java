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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final QuizSubmissionRepository submissionRepository;
    private final QuizSubmissionGroupRepository groupRepository;
    private final UserRepository userRepository;

    @Transactional
    public List<GroupedHistoryDto> getAllGroupedHistories(Long userId) {
        validateUser(userId);

        // 유저의 모든 퀴즈 제출 그룹 조회
        List<QuizSubmissionGroup> groups = groupRepository.findByUser_Id(userId);

        AtomicInteger totalScore = new AtomicInteger();

        // 책 기준으로 그룹핑
        Map<Book, List<QuizSubmissionGroup>> bookToGroups = groups.stream()
                .collect(Collectors.groupingBy(QuizSubmissionGroup::getBook));

        // 책별로 그룹핑된 결과를 순회하면서 Dto로 변환
        return bookToGroups.entrySet().stream().map(entry -> {
            Book book = entry.getKey();
            List<QuizSubmissionGroup> groupList = entry.getValue();

            // 그룹 내 히스토리들 생성 (각 그룹 = 10문제 단위)
            List<HistoryResponseDto> histories = groupList.stream()
                    .sorted(Comparator.comparingInt(QuizSubmissionGroup::getGroupIndex))
                    .map(group -> {
                        List<QuizSubmission> submissions = group.getSubmissions();

                        // 총 점수 계산
                        int score = submissions.stream()
                                .filter(QuizSubmission::isCorrect)
                                .mapToInt(s -> s.getQuiz().getScore())
                                .sum();

                        totalScore.addAndGet(score);

                        // 퀴즈 유형 추출 (첫 번째 문제 기준)
                        String questionTypeLabel = submissions.stream()
                                .map(s -> s.getQuiz().getQuestionType())
                                .findFirst()

                                .map(type -> switch (type) {
                                    case MULTIPLE_CHOICE -> "객관식";
                                    case SHORT_ANSWER -> "주관식";
                                    case TRUE_FALSE -> "O/X";
                                })
                                .orElse("알 수 없음");



                        return HistoryResponseDto.builder()
                                .id(group.getId())
                                .bookId(book.getId())
                                .bookTitle(book.getTitle())
                                .score(score)
                                .submittedAt(group.getSubmittedAt() != null ? group.getSubmittedAt().toString() : "")
                                .groupIndex(group.getGroupIndex())
                                .quizType(questionTypeLabel)
                                .build();
                    })
                    .toList();

            // 최종 GroupedHistoryDto로 묶기 (책 기준)
            return GroupedHistoryDto.builder()
                    .bookId(book.getId())
                    .title(book.getTitle())
                    .author(book.getAuthor())
                    .totalScore(totalScore.get())
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

        List<QuizSubmission> submissions = group.getSubmissions();
        int scoreToDeduct = submissions.stream()
                .filter(QuizSubmission::isCorrect)
                .mapToInt(s -> s.getQuiz().getScore())
                .sum();

        group.getUser().updateScore(group.getUser().getScore() - scoreToDeduct);
        userRepository.save(group.getUser());

        groupRepository.delete(group);
    }

    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
