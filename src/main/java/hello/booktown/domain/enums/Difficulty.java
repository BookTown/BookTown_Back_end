package hello.booktown.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum Difficulty {
    EASY, MEDIUM, HARD;

    @JsonCreator
    public static Difficulty from(String value) {
        return Difficulty.valueOf(value.toUpperCase(Locale.ROOT));
    }
}