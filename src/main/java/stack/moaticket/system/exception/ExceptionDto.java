package stack.moaticket.system.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
public class ExceptionDto {
    private String message;
    private Integer statusCode;
    private Object details;      // optional
    private LocalDateTime timestamp;

    public ExceptionDto(MoaExceptionType type, Object details) {
        this.message = type.getMessage();
        this.statusCode = type.getStatusCode();
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public ExceptionDto(MoaExceptionType type) {
        this(type, null);
    }
}
