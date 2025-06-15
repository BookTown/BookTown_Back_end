package hello.booktown.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import hello.booktown.exception.ErrorResponse;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 중복 아이디 예외 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<?> handleNumberFormatException(NumberFormatException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "잘못된 토큰 형식입니다. 유효한 사용자 ID가 아닙니다."
        ));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ErrorResponse response = new ErrorResponse(errorCode.getHttpStatus(), errorCode.getDetail());
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

}