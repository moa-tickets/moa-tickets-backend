package stack.moaticket.system.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class ResponseApiDTO<T> {

    private String status;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ResponseApiDTO<T> success(MessageType type, T data) {
        return ResponseApiDTO.<T>builder().status("success").message(type.getMessage()).data(data).timestamp(LocalDateTime.now()).build();
    }

    public static <T> ResponseApiDTO<T> success(MessageType type) {
        return ResponseApiDTO.<T>builder().status("success").message(type.getMessage()).timestamp(LocalDateTime.now()).build();
    }
}
