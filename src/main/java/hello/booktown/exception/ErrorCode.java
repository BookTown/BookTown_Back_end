package hello.booktown.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 잘못되었습니다"),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "업로드된 파일이 비어있습니다"),
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 사용자 ID입니다"),

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "이미 만료된 토큰입니다."),
    TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "토큰 사용자 정보가 일치하지 않습니다."),

    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),
    NOT_SELF_UPDATE(HttpStatus.FORBIDDEN, "자신의 정보만 수정할 수 있습니다"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필 정보를 찾을 수 없습니다"),

    DUPLICATED_USER(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다"),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 오류가 발생했습니다");

    private final HttpStatus httpStatus;
    private final String detail;
}