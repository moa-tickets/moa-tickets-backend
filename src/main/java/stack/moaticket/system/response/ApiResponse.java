package stack.moaticket.system.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String code;
    private final String message;
    private final LocalDateTime timestamp;
    private final int status;

    private ApiResponse(boolean success, T data, String code, String message, LocalDateTime timestamp, int status) {
        this.success = success;
        this.data = data;
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
        this.status = status;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null, LocalDateTime.now(), 200);
    }

    public static <T> ApiResponse<T> error(String code, String message, LocalDateTime timestamp, int status) {
        return new ApiResponse<>(false, null, code, message, timestamp, status);
    }
}
