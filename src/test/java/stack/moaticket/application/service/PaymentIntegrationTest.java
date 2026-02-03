package stack.moaticket.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import settings.config.TestFixtureConfig;
import settings.support.fixture.*;
import stack.moaticket.application.dto.PaymentDto;
import stack.moaticket.application.facade.PaymentConfirmFacade;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.hall.entity.Hall;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.repository.PaymentRepositoryQueryDsl;
import stack.moaticket.domain.payment.type.PaymentState;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepository;
import stack.moaticket.domain.ticket.type.TicketState;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.toss.dto.TossConfirmResponse;
import stack.moaticket.system.toss.facade.TossPaymentsFacade;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestFixtureConfig.class)
@Sql(value = "/sql/clear.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class PaymentIntegrationTest {

    @Autowired MemberFixture memberFixture;
    @Autowired HallFixture hallFixture;
    @Autowired ConcertFixture concertFixture;
    @Autowired SessionFixture sessionFixture;
    @Autowired TicketFixture ticketFixture;
    @Autowired PaymentFixture paymentFixture;

    @Autowired BookingService bookingService;
    @Autowired PaymentService paymentService;
    @Autowired PaymentConfirmFacade paymentConfirmFacade;

    @Autowired TicketRepository ticketRepository;
    @Autowired PaymentRepositoryQueryDsl paymentRepositoryQueryDsl;

    @MockitoBean TossPaymentsFacade tossPaymentsFacade;

    @Test
    @DisplayName("[prepare][happy] holdToken으로 준비하면 Payment(READY)가 저장되고 amount/orderName/orderId를 반환한다.")
    void prepare_success_persistsReadyPayment() {
        // given
        Member seller = memberFixture.create();
        Member buyer = memberFixture.create();

        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket t1 = ticketFixture.create(seller, session, 80);
        Ticket t2 = ticketFixture.create(seller, session, 80);

        // 먼저 booking hold로 실제 holdToken 만들기
        BookingService.HoldResult hold = bookingService.holdTickets(buyer.getId(), session.getId(), List.of(t1.getId(), t2.getId()));

        PaymentDto.PrepareRequest req = PaymentDto.PrepareRequest.builder()
                .holdToken(hold.holdToken())
                .build();

        // when
        PaymentDto.PrepareResponse res = paymentService.prepare(buyer.getId(), req);

        // then - 응답 검증
        assertThat(res.getOrderId()).isNotBlank();
        assertThat(res.getOrderName()).isNotBlank();
        assertThat(res.getAmount()).isGreaterThan(0);

        // then - DB 검증 (orderId로 payment 재조회)
        Payment saved = paymentRepositoryQueryDsl.findByOrderId(res.getOrderId());
        assertThat(saved).isNotNull();
        assertThat(saved.getState()).isEqualTo(PaymentState.READY);
        assertThat(saved.getMember().getId()).isEqualTo(buyer.getId());
        assertThat(saved.getHoldToken()).isEqualTo(hold.holdToken());
        assertThat(saved.getAmount()).isEqualTo(res.getAmount());
        assertThat(saved.getOrderName()).isEqualTo(res.getOrderName());
    }

    @Test
    @DisplayName("[confirm][happy] Toss DONE 응답이면 finalize되어 Payment=PAID, Ticket=SOLD로 반영된다.")
    void confirm_success_marksPaidAndTicketsSold() {
        // given
        Member seller = memberFixture.create();
        Member buyer = memberFixture.create();

        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket t1 = ticketFixture.create(seller, session, 80);
        Ticket t2 = ticketFixture.create(seller, session, 80);

        BookingService.HoldResult hold = bookingService.holdTickets(buyer.getId(), session.getId(), List.of(t1.getId(), t2.getId()));

        PaymentDto.PrepareResponse prepared = paymentService.prepare(buyer.getId(),
                PaymentDto.PrepareRequest.builder().holdToken(hold.holdToken()).build()
        );

        // Toss confirm mock
        TossConfirmResponse tossRes = mock(TossConfirmResponse.class);
        given(tossRes.getPaymentKey()).willReturn("pay_key_123");
        given(tossRes.getOrderId()).willReturn(prepared.getOrderId());
        given(tossRes.getTotalAmount()).willReturn((long) prepared.getAmount());

        given(tossPaymentsFacade.confirm(anyString(), eq(prepared.getOrderId()), eq((long) prepared.getAmount())))
                .willReturn(tossRes);

        PaymentDto.ConfirmRequest confirmReq = PaymentDto.ConfirmRequest.builder()
                .orderId(prepared.getOrderId())
                .paymentKey("client_payment_key_dummy") // ctx.paymentKey로 넘어가는 값
                .amount(prepared.getAmount())
                .build();

        // when
        PaymentDto.ConfirmResponse res = paymentConfirmFacade.confirm(buyer.getId(), confirmReq);

        // then - 응답
        assertThat(res.getPaymentState()).isEqualTo(PaymentState.PAID);
        assertThat(res.getPaidAt()).isNotNull();
        assertThat(res.getOrderId()).isEqualTo(prepared.getOrderId());

        // then - DB: payment PAID
        Payment paid = paymentRepositoryQueryDsl.findByOrderId(prepared.getOrderId());
        assertThat(paid.getState()).isEqualTo(PaymentState.PAID);
        assertThat(paid.getPaidAt()).isNotNull();
        assertThat(paid.getPaymentKey()).isEqualTo("pay_key_123");

        // then - DB: tickets SOLD & hold cleared
        List<Ticket> soldTickets = List.of(
                ticketFixture.findById(t1.getId()),
                ticketFixture.findById(t2.getId())
        );

        assertThat(soldTickets).allSatisfy(t -> {
            assertThat(t.getState()).isEqualTo(TicketState.SOLD);
            assertThat(t.getHoldToken()).isNull();
            assertThat(t.getExpiresAt()).isNull();
        });

        // Toss 호출도 1회
        verify(tossPaymentsFacade, times(1))
                .confirm(anyString(), eq(prepared.getOrderId()), eq((long) prepared.getAmount()));
    }

    @Test
    @DisplayName("[confirm][idempotency] confirm을 2번 호출해도 최종 상태는 PAID 유지되고(2번째는 alreadyPaid), Toss는 1번만 호출된다.")
    void confirm_idempotent_secondCallSkipsToss_andReturnsPaid() {
        // given
        Member seller = memberFixture.create();
        Member buyer = memberFixture.create();

        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket t1 = ticketFixture.create(seller, session, 80);

        BookingService.HoldResult hold = bookingService.holdTickets(buyer.getId(), session.getId(), List.of(t1.getId()));
        PaymentDto.PrepareResponse prepared = paymentService.prepare(buyer.getId(),
                PaymentDto.PrepareRequest.builder().holdToken(hold.holdToken()).build()
        );

        // Toss confirm mock (1회차에만 쓰임)
        TossConfirmResponse tossRes = mock(TossConfirmResponse.class);
        given(tossRes.getPaymentKey()).willReturn("pay_key_123");
        given(tossRes.getOrderId()).willReturn(prepared.getOrderId());
        given(tossRes.getTotalAmount()).willReturn((long) prepared.getAmount());

        given(tossPaymentsFacade.confirm(anyString(), eq(prepared.getOrderId()), eq((long) prepared.getAmount())))
                .willReturn(tossRes);

        PaymentDto.ConfirmRequest confirmReq = PaymentDto.ConfirmRequest.builder()
                .orderId(prepared.getOrderId())
                .paymentKey("client_payment_key_dummy")
                .amount(prepared.getAmount())
                .build();

        // when - 1차 confirm (실제 결제 승인)
        PaymentDto.ConfirmResponse first = paymentConfirmFacade.confirm(buyer.getId(), confirmReq);

        // when - 2차 confirm (중복)
        PaymentDto.ConfirmResponse second = paymentConfirmFacade.confirm(buyer.getId(), confirmReq);

        // then
        assertThat(first.getPaymentState()).isEqualTo(PaymentState.PAID);
        assertThat(second.getPaymentState()).isEqualTo(PaymentState.PAID);

        // Toss는 1회만 호출되어야 함 (2번째는 alreadyPaid로 validator 단계에서 반환)
        verify(tossPaymentsFacade, times(1))
                .confirm(anyString(), eq(prepared.getOrderId()), eq((long) prepared.getAmount()));
    }

    @Test
    @DisplayName("[confirm][failure] Toss confirm에서 예외가 발생하면 finalize되지 않고 Ticket은 SOLD로 가지 않는다.")
    void confirm_tossThrows_doesNotFinalize_orSell() {
        // given
        Member seller = memberFixture.create();
        Member buyer = memberFixture.create();

        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket t1 = ticketFixture.create(seller, session, 80);

        BookingService.HoldResult hold = bookingService.holdTickets(buyer.getId(), session.getId(), List.of(t1.getId()));
        PaymentDto.PrepareResponse prepared = paymentService.prepare(buyer.getId(),
                PaymentDto.PrepareRequest.builder().holdToken(hold.holdToken()).build()
        );

        // Toss confirm throws -> facade에서 MoaException(CONFLICT 등)로 래핑될 것
        given(tossPaymentsFacade.confirm(anyString(), eq(prepared.getOrderId()), eq((long) prepared.getAmount())))
                .willThrow(new MoaException(MoaExceptionType.CONFLICT, "toss down"));

        PaymentDto.ConfirmRequest confirmReq = PaymentDto.ConfirmRequest.builder()
                .orderId(prepared.getOrderId())
                .paymentKey("client_payment_key_dummy")
                .amount(prepared.getAmount())
                .build();

        // when & then
        assertThatThrownBy(() -> paymentConfirmFacade.confirm(buyer.getId(), confirmReq))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.CONFLICT);

        // then - ticket SOLD 금지 (PAYMENT_PENDING 그대로여야 함)
        Ticket reloaded = ticketFixture.findById(t1.getId());
        assertThat(reloaded.getState()).isEqualTo(TicketState.PAYMENT_PENDING);

        // payment는 READY 유지(정책) 확인
        Payment payment = paymentRepositoryQueryDsl.findByOrderId(prepared.getOrderId());
        assertThat(payment.getState()).isEqualTo(PaymentState.READY);
    }


    @Test
    @DisplayName("[confirm][atomic] TTL 만료 후 confirm 시도는 Toss 호출 없이 HOLD_EXPIRED로 차단되고, Payment는 READY 유지, Ticket은 SOLD로 가지 않는다.")
    void confirm_ttlExpired_blocksBeforeToss_andKeepsReadyAndHold() {
        // given
        Member seller = memberFixture.create();
        Member buyer = memberFixture.create();

        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket t1 = ticketFixture.create(seller, session, 80);
        Ticket t2 = ticketFixture.create(seller, session, 80);

        // 1) 먼저 HOLD 생성
        BookingService.HoldResult hold = bookingService.holdTickets(
                buyer.getId(), session.getId(), List.of(t1.getId(), t2.getId())
        );

        // 2) prepare로 READY payment 생성
        PaymentDto.PrepareResponse prepared = paymentService.prepare(
                buyer.getId(),
                PaymentDto.PrepareRequest.builder().holdToken(hold.holdToken()).build()
        );

        // 3) TTL 만료 상태 만들기: expiresAt을 과거로 강제 변경
        LocalDateTime expiredAt = LocalDateTime.now().minusMinutes(10);

        Ticket expired1 = ticketFixture.findById(t1.getId());
        Ticket expired2 = ticketFixture.findById(t2.getId());

        expired1.setExpiresAt(expiredAt);
        expired2.setExpiresAt(expiredAt);

        ticketRepository.saveAll(List.of(expired1, expired2));

        // confirm request
        PaymentDto.ConfirmRequest confirmReq = PaymentDto.ConfirmRequest.builder()
                .orderId(prepared.getOrderId())
                .paymentKey("client_payment_key_dummy")
                .amount(prepared.getAmount())
                .build();

        // when & then: TTL 만료이므로 Toss 호출 전에 차단되어야 함
        assertThatThrownBy(() -> paymentConfirmFacade.confirm(buyer.getId(), confirmReq))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.HOLD_EXPIRED);

        // Toss 호출 금지
        verify(tossPaymentsFacade, never())
                .confirm(anyString(), eq(prepared.getOrderId()), eq((long) prepared.getAmount()));

        // Payment는 READY 유지
        Payment payment = paymentRepositoryQueryDsl.findByOrderId(prepared.getOrderId());
        assertThat(payment.getState()).isEqualTo(PaymentState.READY);
        assertThat(payment.getPaidAt()).isNull();

        // Ticket은 SOLD로 가지 않아야 함 (HOLD 유지 + holdToken 유지)
        Ticket reloaded1 = ticketFixture.findById(t1.getId());
        Ticket reloaded2 = ticketFixture.findById(t2.getId());

        assertThat(reloaded1.getState()).isEqualTo(TicketState.HOLD);
        assertThat(reloaded2.getState()).isEqualTo(TicketState.HOLD);

        assertThat(reloaded1.getHoldToken()).isEqualTo(hold.holdToken());
        assertThat(reloaded2.getHoldToken()).isEqualTo(hold.holdToken());
    }

    @Test
    @DisplayName("[quota][concurrency] 동일 사용자가 브라우저 2개로 1장씩 동시 결제하면 1건만 PAID, 다른 1건은 quota로 Toss 호출 전 차단된다.")
    void confirm_concurrent_sameBuyer_exceedLimit_onlyOnePaid_otherBlockedBeforeToss() throws Exception {
        // given
        Member seller = memberFixture.create();
        Member buyer = memberFixture.create();

        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket t1 = ticketFixture.create(seller, session, 100);
        Ticket t2 = ticketFixture.create(seller, session, 100);
        Ticket t3 = ticketFixture.create(seller, session, 100);
        Ticket t4 = ticketFixture.create(seller, session, 100);
        Ticket t5 = ticketFixture.create(seller, session, 100);

        // 먼저 3장 결제 완료 (SOLD 3)
        BookingService.HoldResult hold3 =
                bookingService.holdTickets(buyer.getId(), session.getId(), List.of(t1.getId(), t2.getId(), t3.getId()));

        PaymentDto.PrepareResponse p3 =
                paymentService.prepare(buyer.getId(),
                        PaymentDto.PrepareRequest.builder().holdToken(hold3.holdToken()).build());

        mockTossSuccess(p3);
        paymentConfirmFacade.confirm(buyer.getId(), confirmReq(p3));

        // 브라우저 2개에서 각각 1장 HOLD + prepare
        BookingService.HoldResult holdA =
                bookingService.holdTickets(buyer.getId(), session.getId(), List.of(t4.getId()));
        BookingService.HoldResult holdB =
                bookingService.holdTickets(buyer.getId(), session.getId(), List.of(t5.getId()));

        PaymentDto.PrepareResponse pA =
                paymentService.prepare(buyer.getId(),
                        PaymentDto.PrepareRequest.builder().holdToken(holdA.holdToken()).build());
        PaymentDto.PrepareResponse pB =
                paymentService.prepare(buyer.getId(),
                        PaymentDto.PrepareRequest.builder().holdToken(holdB.holdToken()).build());

        // 동시 2건 중 1건만 Toss까지 가야 하므로, “둘 다” mock 걸지 말고 각각 다른 paymentKey로 성공 응답을 준비해둔다(어차피 1번만 호출됨).
        mockTossSuccess(pA);
        mockTossSuccess(pB);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        AtomicReference<PaymentDto.ConfirmResponse> r1 = new AtomicReference<>();
        AtomicReference<PaymentDto.ConfirmResponse> r2 = new AtomicReference<>();
        AtomicReference<Throwable> e1 = new AtomicReference<>();
        AtomicReference<Throwable> e2 = new AtomicReference<>();

        pool.submit(() -> {
            ready.countDown();
            try {
                start.await();
                r1.set(paymentConfirmFacade.confirm(buyer.getId(), confirmReq(pA)));
            } catch (Throwable t) {
                e1.set(t);
            } finally {
                done.countDown();
            }
        });

        pool.submit(() -> {
            ready.countDown();
            try {
                start.await();
                r2.set(paymentConfirmFacade.confirm(buyer.getId(), confirmReq(pB)));
            } catch (Throwable t) {
                e2.set(t);
            } finally {
                done.countDown();
            }
        });

        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();

        // when
        start.countDown();
        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        // then: 성공 1개, 실패 1개
        int success = 0;
        if (r1.get() != null) success++;
        if (r2.get() != null) success++;
        assertThat(success).isEqualTo(1);

        List<Throwable> failures = new ArrayList<>();
        if (e1.get() != null) failures.add(e1.get());
        if (e2.get() != null) failures.add(e2.get());
        assertThat(failures).hasSize(1);

        assertThat(failures.getFirst())
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.TICKET_LIMIT_EXCEEDED);

        // Payment 상태: PAID 1건, 나머지는 READY 유지(토스 호출 전 차단 정책)
        Payment payA = paymentRepositoryQueryDsl.findByOrderId(pA.getOrderId());
        Payment payB = paymentRepositoryQueryDsl.findByOrderId(pB.getOrderId());

        long paidCount = List.of(payA, payB).stream().filter(p -> p.getState() == PaymentState.PAID).count();
        long readyCount = List.of(payA, payB).stream().filter(p -> p.getState() == PaymentState.READY).count();

        assertThat(paidCount).isEqualTo(1);
        assertThat(readyCount).isEqualTo(1);

        // SOLD는 정확히 4장(기존 3 + 추가 1)
        long soldCount = List.of(t1, t2, t3, t4, t5).stream()
                .map(t -> ticketFixture.findById(t.getId()))
                .filter(t -> t.getState() == TicketState.SOLD)
                .count();
        assertThat(soldCount).isEqualTo(4);

        // Toss confirm 호출 횟수:
        // - p3 1회 + (pA/pB 중 1회) = 총 2회
        verify(tossPaymentsFacade, times(2))
                .confirm(anyString(), anyString(), anyLong());
    }



    // Helpers
    private PaymentDto.ConfirmRequest confirmReq(PaymentDto.PrepareResponse prepared) {
        return PaymentDto.ConfirmRequest.builder()
                .orderId(prepared.getOrderId())
                .paymentKey("client_payment_key_dummy")
                .amount(prepared.getAmount())
                .build();
    }

    private void mockTossSuccess(PaymentDto.PrepareResponse prepared) {
        TossConfirmResponse tossRes = mock(TossConfirmResponse.class);
        given(tossRes.getPaymentKey()).willReturn("pay_key_" + prepared.getOrderId());
        given(tossRes.getOrderId()).willReturn(prepared.getOrderId());
        given(tossRes.getTotalAmount()).willReturn((long) prepared.getAmount());

        given(tossPaymentsFacade.confirm(
                anyString(),
                eq(prepared.getOrderId()),
                eq((long) prepared.getAmount())
        )).willReturn(tossRes);
    }



}
