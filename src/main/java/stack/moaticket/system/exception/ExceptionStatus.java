package stack.moaticket.system.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum ExceptionStatus {

    BAD_REQUEST(400, HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, HttpStatus.FORBIDDEN),
    NOT_FOUND(404, HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(405, HttpStatus.METHOD_NOT_ALLOWED),

    CONFLICT(409, HttpStatus.CONFLICT),
    GONE(410, HttpStatus.GONE),

    UNSUPPORTED_MEDIA_TYPE(415, HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    UNPROCESSABLE_ENTITY(422, HttpStatus.UNPROCESSABLE_ENTITY),

    INTERNAL_SERVER_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_GATEWAY(502, HttpStatus.BAD_GATEWAY),
    SERVICE_UNAVAILABLE(503, HttpStatus.SERVICE_UNAVAILABLE);

    private final Integer status;
    private final HttpStatus httpStatus;

    private static final Map<Integer, HttpStatus> STATUS_MAP = new HashMap<>();

    static {
        for (ExceptionStatus s : ExceptionStatus.values()) {
            STATUS_MAP.put(s.getStatus(), s.getHttpStatus());
        }
    }

    public static HttpStatus getStatus(Integer statusCode) {
        return STATUS_MAP.getOrDefault(statusCode, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
