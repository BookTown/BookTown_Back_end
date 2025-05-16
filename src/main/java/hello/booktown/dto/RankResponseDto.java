// RankResultDto.java
package hello.booktown.dto;

import hello.booktown.domain.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RankResponseDto {
    private Long userId;
    private String username;
    private Integer score;
    private String profileImageUrl;

    public static RankResponseDto from(User user) {
        return RankResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .score(Math.toIntExact(user.getScore()))
                .profileImageUrl(user.getProfileImage())
                .build();
    }
}
