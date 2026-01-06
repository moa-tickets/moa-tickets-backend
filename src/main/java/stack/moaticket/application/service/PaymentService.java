package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stack.moaticket.application.dto.PaymentDto;
import stack.moaticket.domain.member.repository.MemberRepository;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.repository.PaymentRepository;
import stack.moaticket.domain.payment.repository.PaymentRepositoryQueryDsl;
import stack.moaticket.domain.payment.type.PaymentState;
import stack.moaticket.domain.payment_ticket.entity.PaymentTicket;
import stack.moaticket.domain.payment_ticket.repository.PaymentTicketRepository;
import stack.moaticket.domain.session.repository.SessionRepositoryQueryDsl;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepositoryQueryDsl;
import stack.moaticket.domain.ticket.type.TicketState;
import stack.moaticket.domain.ticket_hold.entity.TicketHold;
import stack.moaticket.domain.ticket_hold.repository.TicketHoldRepositoryQueryDsl;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.toss.facade.TossPaymentsFacade;
import stack.moaticket.system.util.TokenGenerator;
import stack.moaticket.system.toss.dto.TossConfirmResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final TicketHoldRepositoryQueryDsl ticketHoldRepositoryQueryDsl;
    private final SessionRepositoryQueryDsl sessionRepositoryQueryDsl;
    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRepositoryQueryDsl paymentRepositoryQueryDsl;
    private final TicketRepositoryQueryDsl ticketRepositoryQueryDsl;
    private final PaymentTicketRepository paymentTicketRepository;
    private final TossPaymentsFacade tossPaymentsFacade;

    @Transactional
    public PaymentDto.PrepareResponse prepare(Long memberId, PaymentDto.PrepareRequest request) {
        if (memberId == null) {
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }
        if (request == null || request.getHoldToken() == null || request.getHoldToken().isBlank()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

        String holdToken = request.getHoldToken();
        LocalDateTime now = LocalDateTime.now();

        // holdToken 검증(만료/소유자/세션 단일성) + hold 목록 조회
        List<TicketHold> holds = validateAndGetHoldsForMember(holdToken, memberId, now);

        // 결제 정보 조회(공연명, 가격)
        Long sessionId = holds.get(0).getSessionId();
        SessionRepositoryQueryDsl.SessionPaymentInfo info = sessionRepositoryQueryDsl.getPaymentInfo(sessionId);
        if (info == null) {
            throw new MoaException(MoaExceptionType.SESSION_NOT_FOUND);
        }

        long amount = (long) info.price() * holds.size();

        // Toss 결제창에 넘길 값 생성
        String orderId = TokenGenerator.generateOrderId();
        String orderName = info.concertName() + " " + holds.size() + "매";

        // Payment(READY) 생성/저장
        Payment payment = Payment.builder()
                .member(memberRepository.getReferenceById(memberId))
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
        if (memberId == null) {
            throw new MoaException(MoaExceptionType.UNAUTHORIZED);
        }
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

        // READY에서만 confirm 허용
        if (payment.getState() != PaymentState.READY) {
            throw new MoaException(MoaExceptionType.PAYMENT_STATE_INVALID);
        }

        // 금액 검증(변조 방지)
        if (payment.getAmount() != reqAmount) {
            throw new MoaException(MoaExceptionType.INVALID_PAYMENT_AMOUNT);
        }

        // holdToken 검증(만료/소유자/세션 단일성) - Toss 승인 전에 먼저 확인
        List<TicketHold> holds = validateAndGetHoldsForMember(payment.getHoldToken(), memberId, now);

        // Toss Payments confirm API 호출(성공 시 아래 finalize 진행, 실패 시 FAILED 처리)
        TossConfirmResponse tossResponse = tossPaymentsFacade.confirm(paymentKey, orderId, reqAmount);
        if (tossResponse == null || tossResponse.getPaymentKey() == null || tossResponse.getPaymentKey().isBlank()) {
            throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
        }

        // tossRes.getPaymentKey()를 신뢰(응답값을 최종값으로)
        finalizeSoldAndPersist(payment, tossResponse.getPaymentKey(), holds, now);

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
    private List<TicketHold> validateAndGetHoldsForMember(String holdToken, Long memberId, LocalDateTime now) {

        // 토큰 유효성 검사
        if (holdToken == null || holdToken.isBlank()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

        List<TicketHold> holds = ticketHoldRepositoryQueryDsl.findByHoldToken(holdToken);
        if (holds.isEmpty()) {
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        // 토큰 만료 확인
        boolean expired = holds.stream().anyMatch(h -> !h.getExpiresAt().isAfter(now));
        if (expired) {
            ticketHoldRepositoryQueryDsl.deleteByHoldToken(holdToken);
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        // 토큰 소유주 = 로그인 유저 확인
        boolean owner = holds.stream().allMatch(h -> h.getMember().getId().equals(memberId));
        if (!owner) {
            throw new MoaException(MoaExceptionType.FORBIDDEN);
        }

        // 모든 티켓이 같은 세션인지 확인
        Long sessionId = holds.get(0).getSessionId();
        boolean allSameSession = holds.stream().allMatch(h -> h.getSessionId().equals(sessionId));
        if (!allSameSession) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }

        return holds;
    }

    // 결제 성공 이후 처리:
    // - 티켓들을 락으로 조회하고 SOLD 처리
    // - payment_ticket 생성
    // - hold 정리
    // - Payment 상태(PAID) 업데이트
    private void finalizeSoldAndPersist(
            Payment payment,
            String paymentKey,
            List<TicketHold> holds,
            LocalDateTime now
    ) {

        // ticketIds 추출 + 정렬
        List<Long> ticketIds = holds.stream()
                .map(TicketHold::getId) // TicketHold PK = ticket_id (MapsId)
                .sorted()
                .toList();

        // ticket 락 + SOLD
        List<Ticket> tickets = ticketRepositoryQueryDsl.getTicketsWithLock(ticketIds);

        if (tickets.size() != ticketIds.size()) {
            payment.setState(PaymentState.FAILED);
            payment.setFailReason("Ticket not found while confirming payment.");
            paymentRepository.save(payment);
            throw new MoaException(MoaExceptionType.TICKET_NOT_FOUND);
        }

        for (Ticket t : tickets) {
            if (t.getState() == TicketState.SOLD) {
                // 여기 들어오면 데이터 꼬임. 멱등은 Payment=PAID에서 처리함.
                payment.setState(PaymentState.FAILED);
                payment.setFailReason("Ticket already sold.");
                paymentRepository.save(payment);
                throw new MoaException(MoaExceptionType.TICKET_ALREADY_SOLD);
            }
        }

        for (Ticket t : tickets) {
            t.setState(TicketState.SOLD);
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

        try {
            paymentTicketRepository.saveAll(paymentTickets);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 중복 confirm 등으로 이미 매핑된 경우
            payment.setState(PaymentState.FAILED);
            payment.setFailReason("Duplicate payment_ticket insert.");
            paymentRepository.save(payment);
            throw new MoaException(MoaExceptionType.CONFLICT);
        }

        // hold 제거
        ticketHoldRepositoryQueryDsl.deleteByHoldToken(payment.getHoldToken());

        // Payment 저장
        paymentRepository.save(payment);
    }

}
