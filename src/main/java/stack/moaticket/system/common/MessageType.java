package stack.moaticket.system.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageType {

    RETRIEVE("조회 완료"),
    CREATE("생성 완료"),
    UPDATE("수정 완료"),
    DELETE("삭제 완료"),
    SEND("요청 완료");

    private final String message;
}
