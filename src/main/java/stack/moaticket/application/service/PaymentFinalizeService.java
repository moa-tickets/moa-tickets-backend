package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.repository.PaymentRepository;
import stack.moaticket.domain.payment.type.PaymentState;
import stack.moaticket.domain.payment_ticket.entity.PaymentTicket;
import stack.moaticket.domain.payment_ticket.repository.PaymentTicketRepository;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepositoryQueryDsl;
import stack.moaticket.domain.ticket.type.TicketState;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFinalizeService {
    private final PaymentRepository paymentRepository;
    private final TicketRepositoryQueryDsl ticketRepositoryQueryDsl;
    private final PaymentTicketRepository paymentTicketRepository;

    @Transactional
    public void finalizeAfterTossPaid(Long paymentId, String paymentKey, Long memberId, LocalDateTime now) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.PAYMENT_NOT_FOUND));

        finalizeSoldAndPersist(payment, paymentKey, memberId, now);
    }

    private void finalizeSoldAndPersist(Payment payment, String paymentKey, Long memberId, LocalDateTime now) {
        String holdToken = payment.getHoldToken();

        // 1. holdToken으로 티켓 락
        List<Ticket> tickets = ticketRepositoryQueryDsl.findTicketsByHoldTokenForUpdate(holdToken);
        if (tickets.isEmpty()) {
            markFailedWithFailReason(payment, "holdToken으로 티켓이 안 잡힘");
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        // 2. 소유자 검증
        boolean owner = tickets.stream().allMatch(t ->
                t.getMember() != null && t.getMember().getId().equals(memberId)
        );
        if (!owner) {
            markFailedWithFailReason(payment, "토큰 소유주가 다름");
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }

        // 3. 만료 검증 (HOLD / PENDING 공통)
        boolean expired = tickets.stream().anyMatch(t -> t.getExpiresAt() == null || !t.getExpiresAt().isAfter(now));
        if (expired) {
            markFailedWithFailReason(payment, "토큰 만료");
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        boolean allPending = tickets.stream().allMatch(t -> t.getState() == TicketState.PAYMENT_PENDING);
        if (!allPending) {
            boolean alreadySold = tickets.stream()
                    .allMatch(t -> t.getState() == TicketState.SOLD);

            if (alreadySold && payment.getState() == PaymentState.PAID) {
                return; // 멱등 finalize 성공
            }

            markFailedWithFailReason(payment, "PAYMENT_PENDING 상태 아님");
            throw new MoaException(MoaExceptionType.TICKET_ALREADY_SOLD);
        }

        // 4. SOLD 처리 + hold 필드 정리
        for (Ticket t : tickets) {
            t.setState(TicketState.SOLD);
            t.setHoldToken(null);
            t.setExpiresAt(null);
        }

        // 5. Payment 확정
        payment.setPaymentKey(paymentKey);
        payment.setPaidAt(now);
        payment.setState(PaymentState.PAID);

        // 6. PaymentTicket 생성
        List<PaymentTicket> paymentTickets = tickets.stream()
                .map(t -> PaymentTicket.builder()
                        .payment(payment)
                        .ticket(t)
                        .build())
                .collect(Collectors.toList());

        List<Long> ticketIds = tickets.stream().map(Ticket::getId).toList();

        try {
            paymentTicketRepository.saveAll(paymentTickets);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 내 payment가 이미 이 티켓들을 다 매핑했으면 => 중복 confirm(멱등 성공)
            long mapped = paymentTicketRepository.countByPaymentIdAndTicketIdIn(payment.getId(), ticketIds);
            if (mapped == ticketIds.size()) {
                // payment 상태 보정 (PAID로)
                payment.setPaymentKey(paymentKey);
                if (payment.getPaidAt() == null) payment.setPaidAt(now);
                payment.setState(PaymentState.PAID);
                payment.setFailReason(null);
                paymentRepository.save(payment);
                return;
            }

            // 그 외의 경우
            payment.setFailReason("payment_ticket conflict or DB error after toss paid.");
            paymentRepository.save(payment);
            throw new MoaException(MoaExceptionType.CONFLICT);

        }

        paymentRepository.save(payment);
    }

    private void markFailedWithFailReason(Payment payment, String reason) {
        payment.setState(PaymentState.FAILED);
        payment.setFailReason(reason);
        paymentRepository.save(payment);
    }

}
