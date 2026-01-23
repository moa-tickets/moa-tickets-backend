package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.PaymentDto;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.repository.PaymentRepository;
import stack.moaticket.domain.payment.repository.PaymentRepositoryQueryDsl;
import stack.moaticket.domain.payment.type.PaymentState;
import stack.moaticket.domain.payment_ticket.entity.PaymentTicket;
import stack.moaticket.domain.payment_ticket.repository.PaymentTicketRepository;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepositoryQueryDsl;
import stack.moaticket.domain.ticket.type.TicketState;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.toss.dto.TossConfirmResponse;
import stack.moaticket.system.toss.facade.TossPaymentsFacade;
import stack.moaticket.system.util.TokenGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private final Validator validator;

    private final MemberService memberService;

    private final PaymentRepository paymentRepository;
    private final PaymentRepositoryQueryDsl paymentRepositoryQueryDsl;
    private final TicketRepositoryQueryDsl ticketRepositoryQueryDsl;
    private final PaymentTicketRepository paymentTicketRepository;
    private final TossPaymentsFacade tossPaymentsFacade;

    @Transactional
    public PaymentDto.PrepareResponse prepare(Long memberId, PaymentDto.PrepareRequest request) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        if (request == null || request.getHoldToken() == null || request.getHoldToken().isBlank()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

        String holdToken = request.getHoldToken();
        LocalDateTime now = LocalDateTime.now();

        // holdToken 검증(만료/소유자/세션 단일성) + hold 목록 조회
        List<Ticket> tickets = validateAndGetHeldTicketsForMember(holdToken, member, now);

        // 결제 정보 조회(공연명, 가격)
        int amount = tickets.stream().mapToInt(t -> t.getSession().getPrice()).sum();

        // Toss 결제창에 넘길 값 생성
        String orderId = TokenGenerator.generateOrderId();
        String orderName = tickets.get(0).getSession().getConcert().getName() + " " + tickets.size() + "매";

        // Payment(READY) 생성/저장
        Payment payment = Payment.builder()
                .member(member)
                .orderId(orderId)
                .orderName(orderName)
                .holdToken(holdToken)
                .amount(amount)
                .state(PaymentState.READY)
                .build();

        paymentRepository.save(payment);

        return PaymentDto.PrepareResponse.builder()
                .orderId(orderId)
                .orderName(orderName)
                .amount(amount)
                .build();
    }

    @Transactional
    public PaymentDto.ConfirmResponse confirm(Long memberId, PaymentDto.ConfirmRequest request) {
        validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED);

        if (request == null
                || request.getOrderId() == null || request.getOrderId().isBlank()
                || request.getPaymentKey() == null || request.getPaymentKey().isBlank()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

        String orderId = request.getOrderId();
        String paymentKey = request.getPaymentKey();
        long reqAmount = request.getAmount();
        LocalDateTime now = LocalDateTime.now();

        if (reqAmount <= 0) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }

        // Payment 조회(멱등/상태전이 보호를 위해 락)
        Payment payment = paymentRepositoryQueryDsl.findByOrderIdForUpdate(orderId);
        if (payment == null) {
            throw new MoaException(MoaExceptionType.PAYMENT_NOT_FOUND);
        }

        // 멱등성: 이미 결제 완료면 동일 응답
        if (payment.getState() == PaymentState.PAID) {
            return PaymentDto.ConfirmResponse.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .paymentState(payment.getState())
                    .paidAt(payment.getPaidAt())
                    .amount(payment.getAmount())
                    .orderName(payment.getOrderName())
                    .build();
        }

        if (!payment.getMember().getId().equals(memberId)) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }

        // READY에서만 confirm 허용
        if (payment.getState() != PaymentState.READY) {
            throw new MoaException(MoaExceptionType.PAYMENT_STATE_INVALID);
        }

        // 금액 검증(변조 방지)
        if (payment.getAmount() != reqAmount) {
            throw new MoaException(MoaExceptionType.INVALID_PAYMENT_AMOUNT);
        }

        // Toss Payments confirm API 호출(성공 시 아래 finalize 진행, 실패 시 FAILED 처리)
        TossConfirmResponse tossResponse = tossPaymentsFacade.confirm(paymentKey, orderId, reqAmount);
        if (tossResponse == null || tossResponse.getPaymentKey() == null || tossResponse.getPaymentKey().isBlank()) {
            throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
        }

        // tossRes.getPaymentKey()를 신뢰(응답값을 최종값으로)
        // TODO TOSS 결제는 했는데 서버 DB 쪽에 READY로 남는 문제 해결해야함 (보정 전략)
        finalizeSoldAndPersist(payment, tossResponse.getPaymentKey(), memberId, now);

        return PaymentDto.ConfirmResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .paymentState(payment.getState())
                .paidAt(payment.getPaidAt())
                .amount(payment.getAmount())
                .orderName(payment.getOrderName())
                .build();
    }

    // holdToken 기반으로 hold 목록을 가져오고, 결제 가능한 상태인지 검증한다.
    // - 만료된 hold가 섞이면 토큰 단위로 정리(delete) 후 실패
    // - 소유자(member) 검증
    // - 세션 단일성 검증
    private List<Ticket> validateAndGetHeldTicketsForMember(String holdToken, Member member, LocalDateTime now) {
        if (holdToken == null || holdToken.isBlank()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

        List<Ticket> tickets = ticketRepositoryQueryDsl.findTicketsByHoldToken(holdToken);
        if (tickets.isEmpty()) {
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        // 소유자 검증 + 상태 검증 + 만료 검증
        boolean owner = tickets.stream().allMatch(t ->
                t.getMember() != null && t.getMember().getId().equals(member.getId())
        );
        if (!owner) throw new MoaException(MoaExceptionType.FORBIDDEN);

        boolean allHold = tickets.stream().allMatch(t -> t.getState() == TicketState.HOLD);
        if (!allHold) throw new MoaException(MoaExceptionType.HOLD_EXPIRED);

        boolean expired = tickets.stream().anyMatch(t -> t.getExpiresAt() == null || !t.getExpiresAt().isAfter(now));
        if (expired) {
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        // 세션 단일성
        Long sessionId = tickets.get(0).getSession().getId();
        boolean allSameSession = tickets.stream().allMatch(t -> t.getSession().getId().equals(sessionId));
        if (!allSameSession) throw new MoaException(MoaExceptionType.VALIDATION_FAILED);

        return tickets;
    }


    // 결제 성공 이후 처리:
    // - 티켓들을 락으로 조회하고 SOLD 처리
    // - payment_ticket 생성
    // - hold 정리
    // - Payment 상태(PAID) 업데이트
    private void finalizeSoldAndPersist(
            Payment payment,
            String paymentKey,
            Long memberId,
            LocalDateTime now
    ) {

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
        // 지금은 DB 저장 안 함. 로그만.
        log.warn("Payment finalize failed. paymentId={}, reason={}", payment.getId(), reason);
    }

}
