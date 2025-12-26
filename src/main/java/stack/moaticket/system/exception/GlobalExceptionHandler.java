package stack.moaticket.system.exception;

import org.springframework.core.env.Environment;
import org.springframework.web.servlet.NoHandlerFoundException;
import stack.moaticket.system.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Arrays;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Environment environment;

    // 비즈니스 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        log.error("CustomException 발생: 코드 = {}, 메시지 = {}", errorCode.getCode(), errorCode.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                errorCode.getCode(),
                errorCode.getMessage(),
                LocalDateTime.now(),
                errorCode.getStatus().value()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    // 검증 실패 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        String validationMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        log.error("ValidationException 발생: 메시지 = {}", validationMessage);

        ApiResponse<Object> response = ApiResponse.error(
                "VALIDATION_FAILED",
                validationMessage,
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // 404 예외 처리 (존재하지 않는 엔드포인트 요청)
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFoundException(NoHandlerFoundException ex) {
        log.warn("404 Not Found: 요청 URL = {}", ex.getRequestURL());

        ApiResponse<Object> response = ApiResponse.error(
                "NOT_FOUND",
                "요청한 리소스를 찾을 수 없습니다.",
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    // 그 외 모든 예외 처리 (Fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUnexpectedException(Exception ex) {
        // 상세 로그는 항상 남김
        log.error("알 수 없는 예외 발생", ex);

        // 메시지 결정: prod 환경이면 일반 메시지, dev 환경이면 상세 메시지
        String message = isProdProfile()
                ? "서버 내부 오류"
                : ex.getMessage();

        ApiResponse<Object> response = ApiResponse.error(
                "INTERNAL_SERVER_ERROR",
                message,
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private boolean isProdProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.asList(activeProfiles).contains("prod");
    }

}
