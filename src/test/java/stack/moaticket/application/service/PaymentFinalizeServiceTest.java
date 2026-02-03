package stack.moaticket.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.repository.PaymentRepository;
import stack.moaticket.domain.payment.type.PaymentState;
import stack.moaticket.domain.payment_ticket.repository.PaymentTicketRepository;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepositoryQueryDsl;
import stack.moaticket.domain.ticket.type.TicketState;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PaymentFinalizeServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TicketRepositoryQueryDsl ticketRepositoryQueryDsl;

    @Mock
    private PaymentTicketRepository paymentTicketRepository;

    private PaymentFinalizeService paymentFinalizeService;

    @BeforeEach
    void setUp() {
        paymentFinalizeService = new PaymentFinalizeService(
                paymentRepository,
                ticketRepositoryQueryDsl,
                paymentTicketRepository
        );
    }

    @Test
    @DisplayName("paymentId로 Payment를 찾지 못하면 PAYMENT_NOT_FOUND 예외가 발생한다.")
    void finalizeAfterTossPaid_paymentNotFound_throwsPaymentNotFound() {
        // given
        Long paymentId = 1L;

        given(paymentRepository.findById(paymentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentFinalizeService.finalizeAfterTossPaid(paymentId, "toss_pk", 1L, LocalDateTime.now()))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.PAYMENT_NOT_FOUND);

        then(ticketRepositoryQueryDsl).shouldHaveNoInteractions();
        then(paymentTicketRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("holdToken으로 티켓이 조회되지 않으면 FAILED로 저장 후 HOLD_EXPIRED 예외가 발생한다.")
    void finalizeAfterTossPaid_ticketsEmpty_marksFailedAndThrowsHoldExpired() {
        // given
        Long paymentId = 1L;
        Long memberId = 10L;
        String paymentKey = "toss_pk";
        LocalDateTime now = LocalDateTime.of(2026, 1, 26, 12, 0);

        Payment payment = mock(Payment.class);
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

        given(payment.getHoldToken()).willReturn("hold_123");

        given(ticketRepositoryQueryDsl.findTicketsByHoldTokenForUpdate("hold_123"))
                .willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> paymentFinalizeService.finalizeAfterTossPaid(paymentId, paymentKey, memberId, now))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.HOLD_EXPIRED);

        // FAILED + failReason 저장 시도
        then(payment).should().setState(PaymentState.FAILED);
        then(payment).should().setFailReason("holdToken으로 티켓이 안 잡힘");
        then(paymentRepository).should().save(payment);

        then(paymentTicketRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("티켓 소유주가 다르면 FAILED로 저장 후 FORBIDDEN 예외가 발생한다.")
    void finalizeAfterTossPaid_ownerMismatch_marksFailedAndThrowsForbidden() {
        // given
        Long paymentId = 1L;
        Long memberId = 10L;
        LocalDateTime now = LocalDateTime.of(2026, 1, 26, 12, 0);

        Payment payment = mock(Payment.class);
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(payment.getHoldToken()).willReturn("hold_123");

        Ticket t1 = mock(Ticket.class);
        Member other = mock(Member.class);
        given(other.getId()).willReturn(999L);
        given(t1.getMember()).willReturn(other);

        given(ticketRepositoryQueryDsl.findTicketsByHoldTokenForUpdate("hold_123"))
                .willReturn(List.of(t1));

        // when & then
        assertThatThrownBy(() -> paymentFinalizeService.finalizeAfterTossPaid(paymentId, "toss_pk", memberId, now))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.FORBIDDEN);

        then(payment).should().setState(PaymentState.FAILED);
        then(payment).should().setFailReason("토큰 소유주가 다름");
        then(paymentRepository).should().save(payment);

        then(paymentTicketRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("티켓의 member가 null이면 FAILED로 저장 후 FORBIDDEN 예외가 발생한다.")
    void finalizeAfterTossPaid_ticketMemberNull_marksFailedAndThrowsForbidden() {
        // given
        Long paymentId = 1L;
        Long memberId = 10L;
        LocalDateTime now = LocalDateTime.of(2026, 1, 26, 12, 0);

        Payment payment = mock(Payment.class);
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(payment.getHoldToken()).willReturn("hold_123");

        Ticket t1 = mock(Ticket.class);
        given(t1.getMember()).willReturn(null);
        given(ticketRepositoryQueryDsl.findTicketsByHoldTokenForUpdate("hold_123"))
                .willReturn(List.of(t1));

        // when & then
        assertThatThrownBy(() -> paymentFinalizeService.finalizeAfterTossPaid(paymentId, "toss_pk", memberId, now))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.FORBIDDEN);

        then(payment).should().setState(PaymentState.FAILED);
        then(payment).should().setFailReason("토큰 소유주가 다름");
        then(paymentRepository).should().save(payment);
    }

    @Test
    @DisplayName("티켓이 만료되었으면 FAILED로 저장 후 HOLD_EXPIRED 예외가 발생한다.")
    void finalizeAfterTossPaid_expired_marksFailedAndThrowsHoldExpired() {
        // given
        Long paymentId = 1L;
        Long memberId = 10L;
        LocalDateTime now = LocalDateTime.of(2026, 1, 26, 12, 0);

        Payment payment = mock(Payment.class);
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(payment.getHoldToken()).willReturn("hold_123");

        Ticket t1 = mock(Ticket.class);
        Member owner = mock(Member.class);
        given(owner.getId()).willReturn(memberId);
        given(t1.getMember()).willReturn(owner);

        // expiresAt == null => expired
        given(t1.getExpiresAt()).willReturn(null);

        given(ticketRepositoryQueryDsl.findTicketsByHoldTokenForUpdate("hold_123"))
                .willReturn(List.of(t1));

        // when & then
        assertThatThrownBy(() -> paymentFinalizeService.finalizeAfterTossPaid(paymentId, "toss_pk", memberId, now))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.HOLD_EXPIRED);

        then(payment).should().setState(PaymentState.FAILED);
        then(payment).should().setFailReason("토큰 만료");
        then(paymentRepository).should().save(payment);

        then(paymentTicketRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("티켓이 HOLD 상태가 아니면 FAILED로 저장 후 TICKET_ALREADY_SOLD 예외가 발생한다.")
    void finalizeAfterTossPaid_notAllHold_marksFailedAndThrowsTicketAlreadySold() {
        // given
        Long paymentId = 1L;
        Long memberId = 10L;
        LocalDateTime now = LocalDateTime.of(2026, 1, 26, 12, 0);

        Payment payment = mock(Payment.class);
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(payment.getHoldToken()).willReturn("hold_123");

        Ticket t1 = mock(Ticket.class);
        Member owner = mock(Member.class);
        given(owner.getId()).willReturn(memberId);
        given(t1.getMember()).willReturn(owner);

        given(t1.getExpiresAt()).willReturn(now.plusMinutes(5));
        given(t1.getState()).willReturn(TicketState.SOLD); // HOLD 아님

        given(ticketRepositoryQueryDsl.findTicketsByHoldTokenForUpdate("hold_123"))
                .willReturn(List.of(t1));

        // when & then
        assertThatThrownBy(() -> paymentFinalizeService.finalizeAfterTossPaid(paymentId, "toss_pk", memberId, now))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.TICKET_ALREADY_SOLD);

        then(payment).should().setState(PaymentState.FAILED);
        then(payment).should().setFailReason("PAYMENT_PENDING 상태 아님");
        then(paymentRepository).should().save(payment);

        then(paymentTicketRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("정상 케이스면 티켓을 SOLD로 전이하고 Payment를 PAID로 저장하며 PaymentTicket을 저장한다.")
    void finalizeAfterTossPaid_success_marksSoldAndPaid_andSavesPaymentTickets() {
        // given
        Long paymentId = 1L;
        Long memberId = 10L;
        String paymentKey = "toss_pk";
        LocalDateTime now = LocalDateTime.of(2026, 1, 26, 12, 0);

        Payment payment = mock(Payment.class);
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(payment.getHoldToken()).willReturn("hold_123");

        Ticket t1 = mock(Ticket.class);
        Member owner = mock(Member.class);
        given(owner.getId()).willReturn(memberId);
        given(t1.getMember()).willReturn(owner);
        given(t1.getExpiresAt()).willReturn(now.plusMinutes(5));
        given(t1.getState()).willReturn(TicketState.HOLD);

        given(ticketRepositoryQueryDsl.findTicketsByHoldTokenForUpdate("hold_123"))
                .willReturn(List.of(t1));

        // when
        paymentFinalizeService.finalizeAfterTossPaid(paymentId, paymentKey, memberId, now);

        // then - ticket sold + hold fields clear
        then(t1).should().setState(TicketState.SOLD);
        then(t1).should().setHoldToken(null);
        then(t1).should().setExpiresAt(null);

        // then - payment paid
        then(payment).should().setPaymentKey(paymentKey);
        then(payment).should().setPaidAt(now);
        then(payment).should().setState(PaymentState.PAID);

        then(paymentTicketRepository).should().saveAll(anyList());
        then(paymentRepository).should().save(payment);
    }

    @Test
    @DisplayName("paymentTicket 저장 중 DataIntegrityViolationException이고, 내 payment가 이미 전부 매핑했으면 멱등 성공으로 보정 저장 후 종료한다.")
    void finalizeAfterTossPaid_duplicateMapping_idempotentSuccess_savesPaymentAndReturns() {
        // given
        Long paymentId = 1L;
        Long memberId = 10L;
        String paymentKey = "toss_pk";
        LocalDateTime now = LocalDateTime.of(2026, 1, 26, 12, 0);

        Payment payment = mock(Payment.class);
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(payment.getHoldToken()).willReturn("hold_123");
        given(payment.getId()).willReturn(paymentId);
        given(payment.getPaidAt()).willReturn(null); // 보정 로직 타게

        Ticket t1 = mock(Ticket.class);
        Member owner = mock(Member.class);
        given(owner.getId()).willReturn(memberId);
        given(t1.getMember()).willReturn(owner);
        given(t1.getExpiresAt()).willReturn(now.plusMinutes(5));
        given(t1.getState()).willReturn(TicketState.HOLD);
        given(t1.getId()).willReturn(101L);

        given(ticketRepositoryQueryDsl.findTicketsByHoldTokenForUpdate("hold_123"))
                .willReturn(List.of(t1));

        willThrow(new DataIntegrityViolationException("dup"))
                .given(paymentTicketRepository).saveAll(anyList());

        // mapped == ticketIds.size() == 1
        given(paymentTicketRepository.countByPaymentIdAndTicketIdIn(eq(paymentId), anyList()))
                .willReturn(1L);

        // when
        paymentFinalizeService.finalizeAfterTossPaid(paymentId, paymentKey, memberId, now);

        // then (보정 저장)
        then(payment).should(atLeastOnce()).setPaymentKey(paymentKey);
        then(payment).should(atLeastOnce()).setState(PaymentState.PAID);
        then(payment).should().setFailReason(null);   // 이건 catch에서 1번만 호출되므로 1회 유지 가능
        then(payment).should(atLeastOnce()).setPaidAt(now); // paidAt은 null이면 catch에서 설정하므로 2번 가능/1번 가능

        then(paymentRepository).should(atLeastOnce()).save(payment);
    }

    @Test
    @DisplayName("paymentTicket 저장 중 DataIntegrityViolationException이고, 내 payment 매핑이 완전하지 않으면 failReason 저장 후 CONFLICT 예외가 발생한다.")
    void finalizeAfterTossPaid_conflictAfterTossPaid_marksFailReasonAndThrowsConflict() {
        // given
        Long paymentId = 1L;
        Long memberId = 10L;
        String paymentKey = "toss_pk";
        LocalDateTime now = LocalDateTime.of(2026, 1, 26, 12, 0);

        Payment payment = mock(Payment.class);
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(payment.getHoldToken()).willReturn("hold_123");
        given(payment.getId()).willReturn(paymentId);

        Ticket t1 = mock(Ticket.class);
        Member owner = mock(Member.class);
        given(owner.getId()).willReturn(memberId);
        given(t1.getMember()).willReturn(owner);
        given(t1.getExpiresAt()).willReturn(now.plusMinutes(5));
        given(t1.getState()).willReturn(TicketState.HOLD);
        given(t1.getId()).willReturn(101L);

        given(ticketRepositoryQueryDsl.findTicketsByHoldTokenForUpdate("hold_123"))
                .willReturn(List.of(t1));

        willThrow(new DataIntegrityViolationException("dup"))
                .given(paymentTicketRepository).saveAll(anyList());

        // mapped != ticketIds.size() => 0
        given(paymentTicketRepository.countByPaymentIdAndTicketIdIn(eq(paymentId), anyList()))
                .willReturn(0L);

        // when & then
        assertThatThrownBy(() -> paymentFinalizeService.finalizeAfterTossPaid(paymentId, paymentKey, memberId, now))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.CONFLICT);

        then(payment).should().setFailReason("payment_ticket conflict or DB error after toss paid.");
        then(paymentRepository).should().save(payment);
    }

    @Test
    @DisplayName("멱등 보정에서 paidAt이 이미 있으면 catch에서 setPaidAt을 추가로 호출하지 않는다(총 1회 호출)")
    void finalizeAfterTossPaid_idempotentSuccess_paidAtAlreadyExists_setPaidAtCalledOnce() {
        // given
        Long paymentId = 1L;
        Long memberId = 10L;
        String paymentKey = "toss_pk";
        LocalDateTime now = LocalDateTime.of(2026, 1, 26, 12, 0);
        LocalDateTime alreadyPaidAt = now.minusMinutes(1);

        Payment payment = mock(Payment.class);
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(payment.getHoldToken()).willReturn("hold_123");
        given(payment.getId()).willReturn(paymentId);

        // catch 분기 조건: paidAt != null
        given(payment.getPaidAt()).willReturn(alreadyPaidAt);

        Ticket t1 = mock(Ticket.class);
        Member owner = mock(Member.class);
        given(owner.getId()).willReturn(memberId);
        given(t1.getMember()).willReturn(owner);
        given(t1.getExpiresAt()).willReturn(now.plusMinutes(5));
        given(t1.getState()).willReturn(TicketState.PAYMENT_PENDING);
        given(t1.getId()).willReturn(101L);

        given(ticketRepositoryQueryDsl.findTicketsByHoldTokenForUpdate("hold_123"))
                .willReturn(List.of(t1));

        willThrow(new org.springframework.dao.DataIntegrityViolationException("dup"))
                .given(paymentTicketRepository).saveAll(anyList());

        given(paymentTicketRepository.countByPaymentIdAndTicketIdIn(eq(paymentId), anyList()))
                .willReturn(1L);

        // when
        paymentFinalizeService.finalizeAfterTossPaid(paymentId, paymentKey, memberId, now);

        // then
        // try 구간에서 1번은 무조건 호출됨.
        // paidAt != null이면 catch에서는 추가 호출이 없어야 하므로 "총 1번"이어야 함
        then(payment).should(times(1)).setPaidAt(now);

        then(paymentRepository).should(atLeastOnce()).save(payment);
    }

    @Test
    @DisplayName("멱등 보정에서 paidAt이 null이면 catch에서 setPaidAt을 한 번 더 호출한다(총 2회 호출)")
    void finalizeAfterTossPaid_idempotentSuccess_paidAtNull_setPaidAtCalledTwice() {
        // given
        Long paymentId = 1L;
        Long memberId = 10L;
        String paymentKey = "toss_pk";
        LocalDateTime now = LocalDateTime.of(2026, 1, 26, 12, 0);

        Payment payment = mock(Payment.class);
        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));
        given(payment.getHoldToken()).willReturn("hold_123");
        given(payment.getId()).willReturn(paymentId);

        // catch 분기 조건: paidAt == null
        given(payment.getPaidAt()).willReturn(null);

        Ticket t1 = mock(Ticket.class);
        Member owner = mock(Member.class);
        given(owner.getId()).willReturn(memberId);
        given(t1.getMember()).willReturn(owner);
        given(t1.getExpiresAt()).willReturn(now.plusMinutes(5));
        given(t1.getState()).willReturn(TicketState.HOLD);
        given(t1.getId()).willReturn(101L);

        given(ticketRepositoryQueryDsl.findTicketsByHoldTokenForUpdate("hold_123"))
                .willReturn(List.of(t1));

        willThrow(new org.springframework.dao.DataIntegrityViolationException("dup"))
                .given(paymentTicketRepository).saveAll(anyList());

        given(paymentTicketRepository.countByPaymentIdAndTicketIdIn(eq(paymentId), anyList()))
                .willReturn(1L);

        // when
        paymentFinalizeService.finalizeAfterTossPaid(paymentId, paymentKey, memberId, now);

        // then
        then(payment).should(times(2)).setPaidAt(now);
        then(paymentRepository).should(atLeastOnce()).save(payment);
    }

}
