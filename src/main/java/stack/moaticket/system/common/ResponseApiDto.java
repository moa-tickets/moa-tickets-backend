package stack.moaticket.system.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class ResponseApiDto<T> {

    private String status;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ResponseApiDto<T> success(MessageType type, T data) {
        return ResponseApiDto.<T>builder().status("success").message(type.getMessage()).data(data).timestamp(LocalDateTime.now()).build();
    }

    public static <T> ResponseApiDto<T> success(MessageType type) {
        return ResponseApiDto.<T>builder().status("success").message(type.getMessage()).timestamp(LocalDateTime.now()).build();
    }
}
