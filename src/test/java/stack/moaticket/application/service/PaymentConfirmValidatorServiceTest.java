package stack.moaticket.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import stack.moaticket.application.dto.PaymentDto;
import stack.moaticket.application.model.ConfirmContext;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.repository.PaymentRepositoryQueryDsl;
import stack.moaticket.domain.ticket.dto.TicketHoldDto;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepositoryQueryDsl;
import stack.moaticket.domain.ticket.type.TicketState;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmValidatorServiceTest {

    // 실제 객체
    private final Validator validator = new Validator();

    @Mock
    private MemberService memberService;

    @Mock
    private PaymentRepositoryQueryDsl paymentRepositoryQueryDsl;
    @Mock
    private TicketRepositoryQueryDsl ticketRepositoryQueryDsl;

    private PaymentConfirmValidatorService validatorService;

    @BeforeEach
    void setUp() {
        validatorService = new PaymentConfirmValidatorService(
                validator,
                memberService,
                paymentRepositoryQueryDsl,
                ticketRepositoryQueryDsl
        );
    }

    @Test
    @DisplayName("회원이 존재하지 않으면 MEMBER_NOT_FOUND 예외가 발생한다.")
    void validate_memberNotFound() {
        // given
        given(memberService.findById(1L)).willReturn(null);

        // when & then
        assertThatThrownBy(() ->
                validatorService.confirmPrepare(1L, validRequest())
        )
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("회원 상태가 ACTIVE가 아니면 UNAUTHORIZED 예외가 발생한다.")
    void validate_memberNotActive() {
        // given
        Member member = mock(Member.class);
        given(member.getState()).willReturn(MemberState.BLOCKED);
        given(memberService.findById(1L)).willReturn(member);

        // when & then
        assertThatThrownBy(() ->
                validatorService.confirmPrepare(1L, validRequest())
        )
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.UNAUTHORIZED);
    }

    @Test
    @DisplayName("request가 null이면 MISMATCH_PARAMETER 예외가 발생한다.")
    void validate_requestNull() {
        // given
        givenActiveMember(1L);

        // when & then
        assertThatThrownBy(() ->
                validatorService.confirmPrepare(1L, null)
        )
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MISMATCH_PARAMETER);
    }

    @Test
    @DisplayName("orderId가 blank이면 MISMATCH_PARAMETER 예외가 발생한다.")
    void validate_orderIdBlank() {
        // given
        givenActiveMember(1L);

        PaymentDto.ConfirmRequest req = PaymentDto.ConfirmRequest.builder()
                .orderId("") // blank
                .paymentKey("payment-key")
                .amount(1000L)
                .build();

        // when & then
        assertThatThrownBy(() -> validatorService.confirmPrepare(1L, req))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MISMATCH_PARAMETER);

        // then (추가 검증: orderId 검증에서 바로 끊겨서 DB 조회는 없어야 함)
        then(paymentRepositoryQueryDsl).should(never()).findByOrderIdForUpdate(anyString());

    }

    @Test
    @DisplayName("paymentKey가 blank이면 MISMATCH_PARAMETER 예외가 발생한다.")
    void validate_paymentKeyBlank() {
        // given
        givenActiveMember(1L);

        PaymentDto.ConfirmRequest req = PaymentDto.ConfirmRequest.builder()
                .orderId("order-1")
                .paymentKey("") // blank
                .amount(1000L)
                .build();

        // when & then
        assertThatThrownBy(() -> validatorService.confirmPrepare(1L, req))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MISMATCH_PARAMETER);

        // then (DB 조회 없음)
        then(paymentRepositoryQueryDsl).should(never()).findByOrderIdForUpdate(anyString());
    }

    @Test
    @DisplayName("결제가 존재하지 않으면 PAYMENT_NOT_FOUND 예외가 발생한다.")
    void validate_paymentNotFound() {
        // given
        givenActiveMember(1L);
        given(paymentRepositoryQueryDsl.findByOrderIdForUpdate(anyString()))
                .willReturn(null);

        // when & then
        assertThatThrownBy(() ->
                validatorService.confirmPrepare(1L, validRequest())
        )
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("결제 소유자가 다르면 FORBIDDEN 예외가 발생한다.")
    void validate_paymentOwnerMismatch() {
        // given
        givenActiveMember(1L);

        Payment payment = mock(Payment.class);
        given(payment.isOwnedBy(1L)).willReturn(false);

        given(paymentRepositoryQueryDsl.findByOrderIdForUpdate(anyString()))
                .willReturn(payment);

        // when & then
        assertThatThrownBy(() ->
                validatorService.confirmPrepare(1L, validRequest())
        )
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.FORBIDDEN);
    }

    @Test
    @DisplayName("결제가 READY 상태가 아니면 PAYMENT_STATE_INVALID 예외가 발생한다.")
    void validate_paymentNotConfirmable() {
        // given
        givenActiveMember(1L);

        Payment payment = mock(Payment.class);
        given(payment.isOwnedBy(1L)).willReturn(true);
        given(payment.isConfirmable()).willReturn(false);

        given(paymentRepositoryQueryDsl.findByOrderIdForUpdate(anyString()))
                .willReturn(payment);

        // when & then
        assertThatThrownBy(() ->
                validatorService.confirmPrepare(1L, validRequest())
        )
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.PAYMENT_STATE_INVALID);
    }

    @Test
    @DisplayName("요청 금액과 결제 금액이 다르면 INVALID_PAYMENT_AMOUNT 예외가 발생한다.")
    void validate_amountMismatch() {
        // given
        givenActiveMember(1L);

        Payment payment = mock(Payment.class);
        given(payment.isOwnedBy(1L)).willReturn(true);
        given(payment.isConfirmable()).willReturn(true);

        // 요청은 1000인데, 결제 금액이 일치하지 않는 케이스
        given(payment.isAmountEquals(1000L)).willReturn(false);

        String orderId = validRequest().getOrderId();
        given(paymentRepositoryQueryDsl.findByOrderIdForUpdate(orderId))
                .willReturn(payment);

        // when & then
        assertThatThrownBy(() -> validatorService.confirmPrepare(1L, validRequest()))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.INVALID_PAYMENT_AMOUNT);
    }

    @Test
    @DisplayName("정상적인 요청이면 ConfirmContext를 반환한다.")
    void validate_success() {
        // given
        givenActiveMember(1L);

        Payment payment = mock(Payment.class);
        given(payment.isOwnedBy(1L)).willReturn(true);
        given(payment.isConfirmable()).willReturn(true);
        given(payment.isAmountEquals(1000L)).willReturn(true);
        given(payment.isPaid()).willReturn(false);
        given(payment.getId()).willReturn(10L);
        given(payment.getOrderId()).willReturn("order-1");
        given(payment.getHoldToken()).willReturn("hold-1");

        given(paymentRepositoryQueryDsl.findByOrderIdForUpdate("order-1"))
                .willReturn(payment);

        TicketHoldDto ticket = mock(TicketHoldDto.class);

        LocalDateTime now = LocalDateTime.now();
        given(ticket.sessionId()).willReturn(100L);
        given(ticket.state()).willReturn(TicketState.HOLD);

        given(ticketRepositoryQueryDsl.findTicketsDto("hold-1"))
                .willReturn(List.of(ticket));

        given(ticketRepositoryQueryDsl.countByMemberAndSessionAndStates(
                eq(1L), eq(100L), anyList()
        )).willReturn(0L);

        given(ticketRepositoryQueryDsl.updateHoldToPaymentPending(
                anyList(), eq(1L), eq(100L), eq("hold-1"), any(LocalDateTime.class)
        )).willReturn(1L);


        // when
        ConfirmContext ctx =
                validatorService.confirmPrepare(1L, validRequest());

        // then
        assertThat(ctx.paymentId()).isEqualTo(10L);
        assertThat(ctx.orderId()).isEqualTo("order-1");
        assertThat(ctx.alreadyPaid()).isFalse();
    }

    @Test
    @DisplayName("이미 결제된 결제면 alreadyPaid=true로 반환한다.")
    void validate_alreadyPaid() {
        // given
        givenActiveMember(1L);

        Payment payment = mock(Payment.class);
        given(payment.isOwnedBy(1L)).willReturn(true);
        given(payment.isConfirmable()).willReturn(false);
        given(payment.isAmountEquals(1000L)).willReturn(true);
        given(payment.isPaid()).willReturn(true);
        given(payment.getId()).willReturn(10L);
        given(payment.getOrderId()).willReturn("order-1");

        given(paymentRepositoryQueryDsl.findByOrderIdForUpdate("order-1"))
                .willReturn(payment);

        // when
        ConfirmContext ctx =
                validatorService.confirmPrepare(1L, validRequest());

        // then
        assertThat(ctx.alreadyPaid()).isTrue();
    }

    // Helpers

    private Member givenActiveMember(Long memberId) {
        Member member = mock(Member.class);
        given(member.getState()).willReturn(MemberState.ACTIVE);
        given(memberService.findById(memberId)).willReturn(member);
        return member;
    }

    private PaymentDto.ConfirmRequest validRequest() {
        return PaymentDto.ConfirmRequest.builder()
                .orderId("order-1")
                .paymentKey("payment-key")
                .amount(1000L)
                .build();
    }

}