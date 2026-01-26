package stack.moaticket.application.facade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import stack.moaticket.application.dto.PaymentDto;
import stack.moaticket.application.model.ConfirmContext;
import stack.moaticket.application.service.PaymentConfirmValidatorService;
import stack.moaticket.application.service.PaymentFinalizeService;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.repository.PaymentRepositoryQueryDsl;
import stack.moaticket.domain.payment.type.PaymentState;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.toss.dto.TossConfirmResponse;
import stack.moaticket.system.toss.facade.TossPaymentsFacade;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmFacadeTest {

    @Mock
    private PaymentConfirmValidatorService validatorService;

    @Mock
    private TossPaymentsFacade tossPaymentsFacade;

    @Mock
    private PaymentFinalizeService paymentFinalizeService;

    @Mock
    private PaymentRepositoryQueryDsl paymentRepositoryQueryDsl;

    private PaymentConfirmFacade paymentConfirmFacade;

    @BeforeEach
    void setUp() {
        paymentConfirmFacade = new PaymentConfirmFacade(
                validatorService,
                tossPaymentsFacade,
                paymentFinalizeService,
                paymentRepositoryQueryDsl
        );
    }

    @Test
    @DisplayName("이미 결제된 상태(alreadyPaid=true)면 Toss 호출/Finalize 없이 멱등 응답을 반환한다.")
    void confirm_alreadyPaid_returnsIdempotentResponse_withoutTossAndFinalize() {
        // given
        Long memberId = 1L;
        PaymentDto.ConfirmRequest request = PaymentDto.ConfirmRequest.builder()
                .orderId("order_123")
                .paymentKey("payKey_abc")
                .amount(10000L)
                .build();

        ConfirmContext ctx = new ConfirmContext(
                55L, memberId, "order_123", "payKey_abc", 10000L, true
        );
        given(validatorService.validateAndLockPayment(memberId, request)).willReturn(ctx);

        Payment paid = mock(Payment.class);
        LocalDateTime paidAt = LocalDateTime.of(2026, 1, 26, 12, 0);

        given(paymentRepositoryQueryDsl.findByOrderId("order_123")).willReturn(paid);
        given(paid.getId()).willReturn(10L);
        given(paid.getOrderId()).willReturn("order_123");
        given(paid.getState()).willReturn(PaymentState.PAID);
        given(paid.getPaidAt()).willReturn(paidAt);
        given(paid.getAmount()).willReturn(10000L);
        given(paid.getOrderName()).willReturn("콘서트A");

        // when
        PaymentDto.ConfirmResponse response = paymentConfirmFacade.confirm(memberId, request);

        // then
        assertThat(response.getPaymentId()).isEqualTo(10L);
        assertThat(response.getOrderId()).isEqualTo("order_123");
        assertThat(response.getPaymentState()).isEqualTo(PaymentState.PAID);
        assertThat(response.getPaidAt()).isEqualTo(paidAt);
        assertThat(response.getAmount()).isEqualTo(10000L);
        assertThat(response.getOrderName()).isEqualTo("콘서트A");

        then(validatorService).should().validateAndLockPayment(memberId, request);
        then(paymentRepositoryQueryDsl).should().findByOrderId("order_123");

        then(tossPaymentsFacade).shouldHaveNoInteractions();
        then(paymentFinalizeService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("정상 confirm이면 Toss confirm 후 finalize를 호출하고 최신 Payment를 조회해 응답한다.")
    void confirm_success_callsTossAndFinalize_andReturnsLatestPayment() {
        // given
        Long memberId = 1L;
        PaymentDto.ConfirmRequest request = PaymentDto.ConfirmRequest.builder()
                .orderId("order_123")
                .paymentKey("payKey_abc")
                .amount(10000L)
                .build();

        ConfirmContext ctx = new ConfirmContext(
                55L, memberId, "order_123", "payKey_abc", 10000L, false
        );
        given(validatorService.validateAndLockPayment(memberId, request)).willReturn(ctx);

        TossConfirmResponse tossResponse = mock(TossConfirmResponse.class);
        given(tossPaymentsFacade.confirm("payKey_abc", "order_123", 10000L)).willReturn(tossResponse);
        given(tossResponse.getPaymentKey()).willReturn("toss_payment_key_returned");

        Payment paid = mock(Payment.class);
        LocalDateTime paidAt = LocalDateTime.of(2026, 1, 26, 12, 1);

        given(paymentRepositoryQueryDsl.findByOrderId("order_123")).willReturn(paid);
        given(paid.getId()).willReturn(55L);
        given(paid.getOrderId()).willReturn("order_123");
        given(paid.getState()).willReturn(PaymentState.PAID);
        given(paid.getPaidAt()).willReturn(paidAt);
        given(paid.getAmount()).willReturn(10000L);
        given(paid.getOrderName()).willReturn("콘서트A");

        // when
        PaymentDto.ConfirmResponse response = paymentConfirmFacade.confirm(memberId, request);

        // then
        assertThat(response.getPaymentId()).isEqualTo(55L);
        assertThat(response.getOrderId()).isEqualTo("order_123");
        assertThat(response.getPaymentState()).isEqualTo(PaymentState.PAID);
        assertThat(response.getPaidAt()).isEqualTo(paidAt);

        then(validatorService).should().validateAndLockPayment(memberId, request);
        then(tossPaymentsFacade).should().confirm("payKey_abc", "order_123", 10000L);

        // LocalDateTime.now()는 고정값이 아니라 any(LocalDateTime.class)로 검증
        then(paymentFinalizeService).should().finalizeAfterTossPaid(
                eq(55L),
                eq("toss_payment_key_returned"),
                eq(memberId),
                any(LocalDateTime.class)
        );

        then(paymentRepositoryQueryDsl).should().findByOrderId("order_123");
    }

    @Test
    @DisplayName("Toss 응답이 null이면 INTERNAL_SERVER_ERROR 예외가 발생한다.")
    void confirm_tossResponseNull_throwsInternalServerError() {
        // given
        Long memberId = 1L;
        PaymentDto.ConfirmRequest request = PaymentDto.ConfirmRequest.builder()
                .orderId("order_123")
                .paymentKey("payKey_abc")
                .amount(10000L)
                .build();

        ConfirmContext ctx = new ConfirmContext(
                55L, memberId, "order_123", "payKey_abc", 10000L, false
        );
        given(validatorService.validateAndLockPayment(memberId, request)).willReturn(ctx);

        given(tossPaymentsFacade.confirm("payKey_abc", "order_123", 10000L)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> paymentConfirmFacade.confirm(memberId, request))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.INTERNAL_SERVER_ERROR);

        then(paymentFinalizeService).shouldHaveNoInteractions();
        then(paymentRepositoryQueryDsl).should(never()).findByOrderId(anyString());
    }

    @Test
    @DisplayName("Toss 응답의 paymentKey가 null이면 INTERNAL_SERVER_ERROR 예외가 발생한다.")
    void confirm_tossPaymentKeyNull_throwsInternalServerError() {
        // given
        Long memberId = 1L;
        PaymentDto.ConfirmRequest request = PaymentDto.ConfirmRequest.builder()
                .orderId("order_123")
                .paymentKey("payKey_abc")
                .amount(10000L)
                .build();

        ConfirmContext ctx = new ConfirmContext(
                55L, memberId, "order_123", "payKey_abc", 10000L, false
        );
        given(validatorService.validateAndLockPayment(memberId, request)).willReturn(ctx);

        TossConfirmResponse tossResponse = mock(TossConfirmResponse.class);
        given(tossPaymentsFacade.confirm("payKey_abc", "order_123", 10000L)).willReturn(tossResponse);
        given(tossResponse.getPaymentKey()).willReturn(null);

        // when & then
        assertThatThrownBy(() -> paymentConfirmFacade.confirm(memberId, request))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.INTERNAL_SERVER_ERROR);

        then(paymentFinalizeService).shouldHaveNoInteractions();
        then(paymentRepositoryQueryDsl).should(never()).findByOrderId(anyString());
    }

    @Test
    @DisplayName("Toss 응답의 paymentKey가 blank면 INTERNAL_SERVER_ERROR 예외가 발생한다.")
    void confirm_tossPaymentKeyBlank_throwsInternalServerError() {
        // given
        Long memberId = 1L;
        PaymentDto.ConfirmRequest request = PaymentDto.ConfirmRequest.builder()
                .orderId("order_123")
                .paymentKey("payKey_abc")
                .amount(10000L)
                .build();

        ConfirmContext ctx = new ConfirmContext(
                55L, memberId, "order_123", "payKey_abc", 10000L, false
        );
        given(validatorService.validateAndLockPayment(memberId, request)).willReturn(ctx);

        TossConfirmResponse tossResponse = mock(TossConfirmResponse.class);
        given(tossPaymentsFacade.confirm("payKey_abc", "order_123", 10000L)).willReturn(tossResponse);
        given(tossResponse.getPaymentKey()).willReturn("   ");

        // when & then
        assertThatThrownBy(() -> paymentConfirmFacade.confirm(memberId, request))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.INTERNAL_SERVER_ERROR);

        then(paymentFinalizeService).shouldHaveNoInteractions();
        then(paymentRepositoryQueryDsl).should(never()).findByOrderId(anyString());
    }

    @Test
    @DisplayName("Toss confirm에서 예외가 발생하면 예외를 전파하고 finalize/조회는 수행하지 않는다.")
    void confirm_tossThrows_propagates_andDoesNotFinalizeOrRead() {
        // given
        Long memberId = 1L;
        PaymentDto.ConfirmRequest request = PaymentDto.ConfirmRequest.builder()
                .orderId("order_123")
                .paymentKey("payKey_abc")
                .amount(10000L)
                .build();

        ConfirmContext ctx = new ConfirmContext(
                55L, memberId, "order_123", "payKey_abc", 10000L, false
        );
        given(validatorService.validateAndLockPayment(memberId, request)).willReturn(ctx);

        given(tossPaymentsFacade.confirm("payKey_abc", "order_123", 10000L))
                .willThrow(new RuntimeException("toss error"));

        // when & then
        assertThatThrownBy(() -> paymentConfirmFacade.confirm(memberId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("toss error");

        then(paymentFinalizeService).shouldHaveNoInteractions();
        then(paymentRepositoryQueryDsl).should(never()).findByOrderId(anyString());
    }

    @Test
    @DisplayName("finalizeAfterTossPaid에서 예외가 발생하면 예외를 전파하고 결과 조회는 수행하지 않는다.")
    void confirm_finalizeThrows_propagates_andDoesNotRead() {
        // given
        Long memberId = 1L;
        PaymentDto.ConfirmRequest request = PaymentDto.ConfirmRequest.builder()
                .orderId("order_123")
                .paymentKey("payKey_abc")
                .amount(10000L)
                .build();

        ConfirmContext ctx = new ConfirmContext(
                55L, memberId, "order_123", "payKey_abc", 10000L, false
        );
        given(validatorService.validateAndLockPayment(memberId, request)).willReturn(ctx);

        TossConfirmResponse tossResponse = mock(TossConfirmResponse.class);
        given(tossPaymentsFacade.confirm("payKey_abc", "order_123", 10000L)).willReturn(tossResponse);
        given(tossResponse.getPaymentKey()).willReturn("toss_payment_key_returned");

        willThrow(new RuntimeException("finalize error"))
                .given(paymentFinalizeService)
                .finalizeAfterTossPaid(eq(55L), eq("toss_payment_key_returned"), eq(memberId), any(LocalDateTime.class));

        // when & then
        assertThatThrownBy(() -> paymentConfirmFacade.confirm(memberId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("finalize error");

        then(paymentRepositoryQueryDsl).should(never()).findByOrderId(anyString());
    }
}
