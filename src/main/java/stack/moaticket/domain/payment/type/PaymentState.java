package stack.moaticket.domain.payment.type;

public enum PaymentState {
    READY, // prepare 완료, 결제 전
    PAID, // Toss confirm 성공
    FAILED, // confirm 실패
    CANCELED // 취소
}
