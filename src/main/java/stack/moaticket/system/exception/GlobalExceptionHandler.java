package stack.moaticket.system.exception;

import org.springframework.core.env.Environment;
import org.springframework.web.servlet.NoHandlerFoundException;
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
    @ExceptionHandler(MoaException.class)
    public ResponseEntity<ExceptionDto> handleCustomException(MoaExceptionType ex) {

        ExceptionDto response = new ExceptionDto(ex);

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(response);
    }

    // 검증 실패 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionDto> handleValidationException(MethodArgumentNotValidException ex) {
        String validationMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        log.error("ValidationException 발생: 메시지 = {}", validationMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ExceptionDto(MoaExceptionType.MISMATCH_ARGUMENT));
    }

    // 404 예외 처리 (존재하지 않는 엔드포인트 요청)
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ExceptionDto> handleNotFoundException(NoHandlerFoundException ex) {
        log.warn("404 Not Found: 요청 URL = {}", ex.getRequestURL());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ExceptionDto(MoaExceptionType.NOT_FOUND));
    }

    // 그 외 모든 예외 처리 (Fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDto> handleUnexpectedException(Exception ex) {
        // 상세 로그는 항상 남김
        log.error("알 수 없는 예외 발생", ex);

        // 메시지 결정: prod 환경이면 일반 메시지, dev 환경이면 상세 메시지
        String message = isProdProfile()
                ? "서버 내부 오류"
                : ex.getMessage();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ExceptionDto(MoaExceptionType.INTERNAL_SERVER_ERROR));
    }

    private boolean isProdProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.asList(activeProfiles).contains("prod");
    }

}
