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
    private final TossPaymentsFacade tossPaymentsFacade;
    private final PaymentFinalizeService paymentFinalizeService;


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

    public PaymentDto.ConfirmResponse confirm(Long memberId, PaymentDto.ConfirmRequest request) {
        // 1) TX1 : 검증 + payment 락 조회
        ConfirmContext ctx = validateAndLockPayment(memberId, request);

        // 2) TX 밖 : Toss API 호출
        TossConfirmResponse tossResponse = tossPaymentsFacade.confirm(ctx.paymentKey(), ctx.orderId(), ctx.amount());

        if(tossResponse == null || tossResponse.getPaymentKey() == null || tossResponse.getPaymentKey().isBlank()) {
            throw new MoaException(MoaExceptionType.INTERNAL_SERVER_ERROR);
        }

        // 3) TX2 (REQUIRES_NEW) : 확정 finalize
        paymentFinalizeService.finalizeAfterTossPaid(ctx.paymentId(), tossResponse.getPaymentKey(), memberId, LocalDateTime.now());

        // 4) 결과 조회 (payment를 다시 조회해서 최신 상태로)
        Payment paid = paymentRepositoryQueryDsl.findByOrderId(ctx.orderId());
        return PaymentDto.ConfirmResponse.builder()
                .paymentId(paid.getId())
                .orderId(paid.getOrderId())
                .paymentState(paid.getState())
                .paidAt(paid.getPaidAt())
                .amount(paid.getAmount())
                .orderName(paid.getOrderName())
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

        //TODO: 캡슐화 필요
        boolean expired = tickets.stream().anyMatch(t -> t.getExpiresAt() == null || !t.getExpiresAt().isAfter(now));
        if (expired) {
            throw new MoaException(MoaExceptionType.HOLD_EXPIRED);
        }

        // 세션 단일성
        Long sessionId = tickets.getFirst().getSession().getId();
        boolean allSameSession = tickets.stream().allMatch(t -> t.getSession().getId().equals(sessionId));
        if (!allSameSession) throw new MoaException(MoaExceptionType.VALIDATION_FAILED);

        return tickets;
    }

    // TX1: 입력/멤버/Payment 락 검증 메서드
    @Transactional
    protected ConfirmContext validateAndLockPayment(Long memberId, PaymentDto.ConfirmRequest request) {
        // member 검증은 반드시 실행되게!
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        if (request == null
                || request.getOrderId() == null || request.getOrderId().isBlank()
                || request.getPaymentKey() == null || request.getPaymentKey().isBlank()) {
            throw new MoaException(MoaExceptionType.MISMATCH_PARAMETER);
        }

        String orderId = request.getOrderId();
        String paymentKey = request.getPaymentKey();
        long reqAmount = request.getAmount();

        if (reqAmount <= 0) {
            throw new MoaException(MoaExceptionType.VALIDATION_FAILED);
        }

        Payment payment = validator.of(paymentRepositoryQueryDsl.findByOrderIdForUpdate(orderId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.PAYMENT_NOT_FOUND)
                .validateOrThrow(p -> !p.getMember().getId().equals(memberId), MoaExceptionType.FORBIDDEN)
                .validateOrThrow(p -> p.getState() != PaymentState.READY, MoaExceptionType.PAYMENT_STATE_INVALID)
                .validateOrThrow(p -> p.getAmount() != reqAmount, MoaExceptionType.INVALID_PAYMENT_AMOUNT)
                .get();

        // 멱등: 이미 PAID면 여기서 바로 응답 컨텍스트 반환하거나, 바로 응답해도 됨
        if (payment.getState() == PaymentState.PAID) {
            return new ConfirmContext(payment.getId(), member.getId(), payment.getOrderId(), paymentKey, reqAmount);
        }

        return new ConfirmContext(payment.getId(), member.getId(), payment.getOrderId(), paymentKey, reqAmount);
    }

    private record ConfirmContext(
            Long paymentId,
            Long memberId,
            String orderId,
            String paymentKey,
            long amount
    ) {}


}
