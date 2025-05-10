package hello.booktown.service;

import hello.booktown.domain.History;
import hello.booktown.dto.HistoryResponseDto;
import hello.booktown.exception.CustomException;
import hello.booktown.exception.ErrorCode;
import hello.booktown.repository.HistoryRepository;
import hello.booktown.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final UserRepository userRepository;

    public List<HistoryResponseDto> getAllHistories(Long userId) {
        validateUserExists(userId);
        return historyRepository.findByUserId(userId).stream()
                .map(HistoryResponseDto::fromEntity)
                .toList();
    }

    public HistoryResponseDto getHistoryById(Long userId, Long historyId) {
        validateUserExists(userId);
        History history = historyRepository.findByIdAndUserId(historyId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST));
        return HistoryResponseDto.fromEntity(history);
    }

    public void deleteHistory(Long userId, Long historyId) {
        validateUserExists(userId);
        History history = historyRepository.findByIdAndUserId(historyId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST));
        historyRepository.delete(history);
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
