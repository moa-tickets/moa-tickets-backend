package stack.moaticket.system.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.NoHandlerFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.List;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Environment environment;

    // 비즈니스 예외 처리
    @ExceptionHandler(MoaException.class)
    public ResponseEntity<ExceptionDto> handleCustomException(MoaException ex, HttpServletRequest req) {

        if (ex.getType() == MoaExceptionType.TICKET_ALREADY_HELD) {
            // 정상적인 경합 → info/warn, stacktrace X
            log.info("Hold conflict: type={}, path={}, msg={}",
                    ex.getType(), req.getRequestURI(), ex.getMessage());

            return ex.toResponse();
        }

        log.error("MoaException 발생: type={}, message={}",
                ex.getType(), ex.getMessage(), ex);
        return ex.toResponse();
    }

    // 검증 실패 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionDto> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        // 모든 검증 실패 메시지 수집
        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .toList();

        log.error("ValidationException 발생: errors = {}", errors);

        // details에 모든 에러 포함
        ExceptionDto response = new ExceptionDto(
                MoaExceptionType.VALIDATION_FAILED,
                errors  // 여러 검증 실패 메시지 모두 전달
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
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
        log.error("예상치 못한 예외 발생: type={}, message={}",
                ex.getClass().getName(), ex.getMessage(), ex);

        // prod 환경이면 일반 메시지, dev 환경이면 상세 메시지
        Object details = isProdProfile() ? null : ex.getMessage();

        ExceptionDto response = new ExceptionDto(
                MoaExceptionType.INTERNAL_SERVER_ERROR,
                details  // ← dev에서는 ex.getMessage(), prod에서는 null
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
