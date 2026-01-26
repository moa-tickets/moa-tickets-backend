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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeAfterTossPaid(Long paymentId, String paymentKey, Long memberId, LocalDateTime now) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new MoaException(MoaExceptionType.PAYMENT_NOT_FOUND));

        // 기존 finalizeSoldAndPersist 내용을 여기로 옮기거나,
        // PaymentService의 private 메서드를 public으로 바꿔서 주입받아 호출하는 방식은 비추천.
        finalizeSoldAndPersist(payment, paymentKey, memberId, now);
    }

    private void finalizeSoldAndPersist(Payment payment, String paymentKey, Long memberId, LocalDateTime now) {
        String holdToken = payment.getHoldToken();

        // holdToken으로 티켓 락
        List<Ticket> tickets = ticketRepositoryQueryDsl.findTicketsByHoldTokenForUpdate(holdToken);
        if (tickets.isEmpty()) {
            markReadyWithFailReason(payment, "holdToken으로 티켓이 안 잡힘");
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        // 소유자/상태/만료 최종 검증
        boolean owner = tickets.stream().allMatch(t ->
                t.getMember() != null && t.getMember().getId().equals(memberId)
        );
        if (!owner) {
            markReadyWithFailReason(payment, "토큰 소유주가 다름");
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }

        boolean expired = tickets.stream().anyMatch(t -> t.getExpiresAt() == null || !t.getExpiresAt().isAfter(now));
        if (expired) {
            markReadyWithFailReason(payment, "토큰 만료");
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        boolean allHold = tickets.stream().allMatch(t -> t.getState() == TicketState.HOLD);
        if (!allHold) {
            markReadyWithFailReason(payment, "HOLD 상태 아님");
            throw new MoaException(MoaExceptionType.TICKET_ALREADY_SOLD);
        }

        // SOLD 처리 + hold 필드 정리
        for (Ticket t : tickets) {
            t.setState(TicketState.SOLD);
            t.setHoldToken(null);
            t.setExpiresAt(null);
        }

        // Payment 상태 업데이트
        payment.setPaymentKey(paymentKey);
        payment.setPaidAt(now);
        payment.setState(PaymentState.PAID);

        // PaymentTicket 생성
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
            // 1) 내 payment가 이미 이 티켓들을 다 매핑했으면 => 중복 confirm(멱등 성공)
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

            // 2) 그렇지 않다면 => 다른 결제가 이 티켓을 이미 먹었거나, 진짜 DB 문제
            // 여기서는 "FAILED로 박기"보단 READY 유지 + 보정 대상 표시가 더 안전하지만,
            // 사용자에게는 CONFLICT / TRY_AGAIN 같은 응답을 주는 게 맞음
            payment.setFailReason("payment_ticket conflict or DB error after toss paid.");
            paymentRepository.save(payment);

            throw new MoaException(MoaExceptionType.CONFLICT);

        }

        paymentRepository.save(payment);
    }

    private void markReadyWithFailReason(Payment payment, String reason) {
        payment.setState(PaymentState.FAILED);
        payment.setFailReason(reason);
        paymentRepository.save(payment);
    }

}
