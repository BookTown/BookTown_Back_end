package hello.booktown.dto;

import hello.booktown.domain.enums.Difficulty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequestDto {
    private String username;
    private String introduction;
}