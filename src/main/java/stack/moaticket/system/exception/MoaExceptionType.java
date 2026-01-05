package stack.moaticket.system.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MoaExceptionType {

    // 400
    MISMATCH_ARGUMENT("입력 인자 형식이 잘못되었습니다.", 400),
    MISMATCH_PARAMETER("입력 매개변수 형식이 잘못되었습니다.", 400),
    MISMATCH_HEADER("입력 헤더 형식이 잘못되었습니다.", 400),


    VALIDATION_FAILED("요청 값이 올바르지 않습니다.", 400),

    // 403
    INVALID_STREAM_REQUEST("유효하지 않은 스트림 요청입니다.", 403),
    HOLD_TOKEN_MISMATCH("선점 토큰이 일치하지 않습니다.", 403),
    NOT_AUTH("인증되지 않은 사용자입니다",403),

    // 404
    NOT_FOUND("요청한 리소스를 찾을 수 없습니다.", 404),
    TICKET_NOT_FOUND("티켓 정보를 찾을 수 없습니다.", 404),
    SESSION_NOT_FOUND("회차 정보를 찾을 수 없습니다.", 404),
    SEAT_NOT_FOUND("좌석 정보를 찾을 수 없습니다.", 404),
    MEMBER_NOT_FOUND("올바른 사용자를 찾을 수 없습니다", 404),

    // 409
    TICKET_ALREADY_SOLD("이미 판매 완료된 좌석입니다.", 409),
    TICKET_ALREADY_HELD("이미 다른 사용자가 선점한 좌석입니다.", 409),
    ALREADY_QUESTION("이미 등록된 질문입니다.", 409),

    // 410
    HOLD_EXPIRED("좌석 선점 시간이 만료되었습니다.", 410),

    // 500
    INTERNAL_SERVER_ERROR("서버 내부 오류", 500),

    // 502
    STREAM_ERROR("스트림 관련 오류가 발생했습니다.", 502);

    private final String message;
    private final Integer statusCode;
}
