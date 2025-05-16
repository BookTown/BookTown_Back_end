package hello.booktown.service;

import hello.booktown.dto.RankResponseDto;
import hello.booktown.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RankService {

    private final UserRepository userRepository;

    public List<RankResponseDto> getTop3Rank() {
        return userRepository.findTop3ByOrderByScoreDesc().stream()
                .map(RankResponseDto::from)
                .toList();
    }

    public List<RankResponseDto> getAllRank() {
        return userRepository.findAllByOrderByScoreDesc().stream()
                .map(RankResponseDto::from)
                .toList();
    }
}
