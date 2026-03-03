package stack.moaticket.system.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
public class MoaException extends RuntimeException {

    private final MoaExceptionType type;
    private final Integer statusCode;
    private final Object details;

    public MoaException(MoaExceptionType type) {
        super(type.getMessage());
        this.type = type;
        this.statusCode = type.getStatusCode();
        this.details = null;
    }

    public MoaException(MoaExceptionType type, Object details) {
        super(type.getMessage());
        this.type = type;
        this.statusCode = type.getStatusCode();
        this.details = details;
    }

    public ResponseEntity<ExceptionDto> toResponse() {
        HttpStatus status = ExceptionStatus.getStatus(this.statusCode);
        ExceptionDto body = new ExceptionDto(this.type, this.details);
        return ResponseEntity.status(status).body(body);
    }
}
