package stack.moaticket.application.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import settings.config.TestFixtureConfig;
import settings.support.fixture.*;
import stack.moaticket.domain.concert.entity.Concert;
import stack.moaticket.domain.hall.entity.Hall;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.session.entity.Session;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepositoryQueryDsl;
import stack.moaticket.domain.ticket.type.TicketState;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Import(TestFixtureConfig.class)
@Sql(value = "/sql/clear.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class BookingIntegrationTest {

    @Autowired MemberFixture memberFixture;
    @Autowired HallFixture hallFixture;
    @Autowired ConcertFixture concertFixture;
    @Autowired SessionFixture sessionFixture;
    @Autowired TicketFixture ticketFixture;

    @Autowired BookingService bookingService;
    @Autowired TicketRepositoryQueryDsl ticketRepositoryQueryDsl;

    @AfterEach
    void clear(){
        ticketFixture.clear();
        sessionFixture.clear();
        concertFixture.clear();
        hallFixture.clear();
        memberFixture.clear();
    }

    @Test
    @DisplayName("[happy] AVAILABLE ticket을 hold 하면 ticket의 상태가 HOLD로 바뀌고 holdToken/expiresAt이 설정된다.")
    void holdTickets_success(){
        // given
        Member seller = memberFixture.create();
        Member buyer = memberFixture.create();

        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket ticket = ticketFixture.create(seller, session, 80);
        Long ticketId = ticket.getId();

        // when
        BookingService.HoldResult holdResult = bookingService.holdTickets(buyer.getId(), session.getId(), List.of(ticketId));

        // then
        // 반환값 검증
        assertThat(holdResult.holdToken()).isNotBlank();
        assertThat(holdResult.expiresAt()).isNotNull();

        // DB 반영 검증
        Ticket reloaded = ticketFixture.findById(ticketId);
        assertThat(reloaded.getState()).isEqualTo(TicketState.HOLD);
        assertThat(reloaded.getHoldToken()).isEqualTo(holdResult.holdToken());
        assertThat(reloaded.getExpiresAt()).isEqualTo(holdResult.expiresAt());
        assertThat(reloaded.getMember().getId()).isEqualTo(buyer.getId());

    }

    @Test
    @DisplayName("[concurrency] 동일 티켓에 대해 N명이 동시에 hold 시도하면 1명만 성공한다.")
    void holdTickets_concurrent_singleTicket_onlyOneSuccess() throws InterruptedException {
        // given
        Member seller = memberFixture.create();
        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket ticket = ticketFixture.create(seller, session, 80);
        Long ticketId = ticket.getId();

        int threadCount = 20;

        List<Member> buyers = new ArrayList<>();
        for(int i = 0; i < threadCount; i++) buyers.add(memberFixture.create());

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        Queue<String> successTokens = new ConcurrentLinkedQueue<>();
        Queue<MoaExceptionType> failureTypes = new ConcurrentLinkedQueue<>();
        Queue<Throwable> unexpectedErrors = new ConcurrentLinkedQueue<>();


        // when
        for (Member buyer : buyers) {
            pool.submit(()->{
                readyLatch.countDown();
                try {
                    startLatch.await(); // 동시에 출발
                    BookingService.HoldResult holdResult = bookingService.holdTickets(buyer.getId(), session.getId(), List.of(ticketId));
                    successCount.incrementAndGet();
                    successTokens.add(holdResult.holdToken());
                }catch (MoaException e){
                    failureTypes.add(e.getType());
                }catch (Throwable t){
                    unexpectedErrors.add(t);
                }finally {
                    doneLatch.countDown();
                }
            });
        }

        // 모든 스레드 준비될 때까지 기다렸다가 출발
        boolean allReady = readyLatch.await(3, TimeUnit.SECONDS);
        assertThat(allReady).isTrue();

        startLatch.countDown();

        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        // then
        assertThat(finished).isTrue();
        assertThat(unexpectedErrors).isEmpty();

        // 성공은 한 명
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(successTokens).hasSize(1);
        assertThat(successTokens.peek()).isNotBlank();


        // 실패는 threadCount - 1명
        assertThat(failureTypes)
                .allMatch(t -> t == MoaExceptionType.TICKET_ALREADY_HELD);

        // DB 반영
        Ticket reloaded = ticketFixture.findById(ticketId);
        assertThat(reloaded.getState()).isEqualTo(TicketState.HOLD);
        assertThat(reloaded.getHoldToken()).isNotBlank();
        assertThat(reloaded.getHoldToken()).isEqualTo(successTokens.peek());

        // 성공 토큰 = DB holdToken
        assertThat(reloaded.getHoldToken()).isEqualTo(successTokens.peek());
        assertThat(reloaded.getMember()).isNotNull();
    }

    @DisplayName("[concurrency] 서로 다른 사용자가 동일 session의 서로 다른 좌석 2장을 동시에 hold하면 둘 다 성공한다.")
    @Test
    void holdTickets_concurrentDifferentMembersDifferentTickets_bothSuccess() throws Exception {
        // given
        Member seller = memberFixture.create();
        Member buyer1 = memberFixture.create();
        Member buyer2 = memberFixture.create();

        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket ticket1 = ticketFixture.create(seller, session, 80);
        Ticket ticket2 = ticketFixture.create(seller, session, 80);

        Long sessionId = session.getId();
        Long ticketId1 = ticket1.getId();
        Long ticketId2 = ticket2.getId();

        // 동시에 출발시키기 위한 장치
        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicReference<BookingService.HoldResult> r1 = new AtomicReference<>();
        AtomicReference<BookingService.HoldResult> r2 = new AtomicReference<>();
        AtomicReference<Throwable> e1 = new AtomicReference<>();
        AtomicReference<Throwable> e2 = new AtomicReference<>();

        Runnable task1 = () -> {
            ready.countDown();
            try {
                start.await();
                r1.set(bookingService.holdTickets(buyer1.getId(), sessionId, List.of(ticketId1)));
            } catch (Throwable t) {
                e1.set(t);
            } finally {
                done.countDown();
            }
        };

        Runnable task2 = () -> {
            ready.countDown();
            try {
                start.await();
                r2.set(bookingService.holdTickets(buyer2.getId(), sessionId, List.of(ticketId2)));
            } catch (Throwable t) {
                e2.set(t);
            } finally {
                done.countDown();
            }
        };

        pool.submit(task1);
        pool.submit(task2);

        // 두 스레드 모두 준비될 때까지 기다렸다가, 동시에 출발
        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();

        // when
        start.countDown();

        // 완료 대기
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        // then - 둘 다 예외 없이 성공해야 함
        assertThat(e1.get()).isNull();
        assertThat(e2.get()).isNull();

        BookingService.HoldResult hold1 = r1.get();
        BookingService.HoldResult hold2 = r2.get();

        assertThat(hold1).isNotNull();
        assertThat(hold2).isNotNull();
        assertThat(hold1.holdToken()).isNotBlank();
        assertThat(hold2.holdToken()).isNotBlank();
        assertThat(hold1.expiresAt()).isNotNull();
        assertThat(hold2.expiresAt()).isNotNull();

        // then - DB 반영 검증 (각 ticket이 각 buyer에게 HOLD 되어야 함)
        Ticket reloaded1 = ticketFixture.findById(ticketId1);
        Ticket reloaded2 = ticketFixture.findById(ticketId2);

        assertThat(reloaded1.getState()).isEqualTo(TicketState.HOLD);
        assertThat(reloaded2.getState()).isEqualTo(TicketState.HOLD);

        assertThat(reloaded1.getMember().getId()).isEqualTo(buyer1.getId());
        assertThat(reloaded2.getMember().getId()).isEqualTo(buyer2.getId());

        assertThat(reloaded1.getHoldToken()).isEqualTo(hold1.holdToken());
        assertThat(reloaded2.getHoldToken()).isEqualTo(hold2.holdToken());

        assertThat(reloaded1.getExpiresAt()).isEqualTo(hold1.expiresAt());
        assertThat(reloaded2.getExpiresAt()).isEqualTo(hold2.expiresAt());
    }



    @Test
    @DisplayName("서로 다른 member가 서로 다른 ticket을 hold하면 각각의 요청에 대해 ticket의 상태가 HOLD로 바뀌고 holdToken/expiresAt이 설정된다.")
    void holdTickets_concurrent_twoMember_twoTickets_Success() throws InterruptedException {
        // given
        Member seller = memberFixture.create();
        Member buyer1 = memberFixture.create();
        Member buyer2 = memberFixture.create();

        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket ticket1 = ticketFixture.create(seller, session, 80);
        Ticket ticket2 = ticketFixture.create(seller, session, 80);
        Ticket ticket3 = ticketFixture.create(seller, session, 80);
        Ticket ticket4 = ticketFixture.create(seller, session, 80);

        List<Long> ticketIds1 = List.of(ticket1.getId(), ticket2.getId());
        List<Long> ticketIds2 = List.of(ticket3.getId(), ticket4.getId());

        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicReference<BookingService.HoldResult> r1 = new AtomicReference<>();
        AtomicReference<BookingService.HoldResult> r2 = new AtomicReference<>();
        AtomicReference<Throwable> e1 = new AtomicReference<>();
        AtomicReference<Throwable> e2 = new AtomicReference<>();

        Runnable task1 = () -> {
            ready.countDown();
            try {
                start.await();
                r1.set(bookingService.holdTickets(buyer1.getId(), session.getId(), ticketIds1));
            } catch (Throwable t) {
                e1.set(t);
            } finally {
                done.countDown();
            }
        };

        Runnable task2 = () -> {
            ready.countDown();
            try {
                start.await();
                r2.set(bookingService.holdTickets(buyer2.getId(), session.getId(), ticketIds2));
            } catch (Throwable t) {
                e2.set(t);
            } finally {
                done.countDown();
            }
        };

        pool.submit(task1);
        pool.submit(task2);

        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();

        // when
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        // then - 예외 없어야 함
        assertThat(e1.get()).isNull();
        assertThat(e2.get()).isNull();

        BookingService.HoldResult hold1 = r1.get();
        BookingService.HoldResult hold2 = r2.get();

        // then - 반환값 검증
        assertThat(hold1.holdToken()).isNotBlank();
        assertThat(hold1.expiresAt()).isNotNull();
        assertThat(hold2.holdToken()).isNotBlank();
        assertThat(hold2.expiresAt()).isNotNull();

        // then - DB 반영 검증 (각 holdToken으로 티켓 2장씩 조회)
        List<Ticket> heldByBuyer1 = ticketRepositoryQueryDsl.findTicketsByHoldToken(hold1.holdToken());
        List<Ticket> heldByBuyer2 = ticketRepositoryQueryDsl.findTicketsByHoldToken(hold2.holdToken());

        assertThat(heldByBuyer1).hasSize(2);
        assertThat(heldByBuyer2).hasSize(2);

        assertThat(heldByBuyer1).allSatisfy(t -> {
            assertThat(t.getState()).isEqualTo(TicketState.HOLD);
            assertThat(t.getHoldToken()).isEqualTo(hold1.holdToken());
            assertThat(t.getExpiresAt()).isEqualTo(hold1.expiresAt());
            assertThat(t.getMember().getId()).isEqualTo(buyer1.getId());
        });

        assertThat(heldByBuyer2).allSatisfy(t -> {
            assertThat(t.getState()).isEqualTo(TicketState.HOLD);
            assertThat(t.getHoldToken()).isEqualTo(hold2.holdToken());
            assertThat(t.getExpiresAt()).isEqualTo(hold2.expiresAt());
            assertThat(t.getMember().getId()).isEqualTo(buyer2.getId());
        });

    }

    @Test
    @DisplayName("[concurrency] 같은 2장(A,B)을 두 명이 반대 순서로 동시에 hold하면 데드락 없이 1명만 성공하고 2장은 동일 토큰으로 HOLD 된다.")
    void holdTickets_concurrent_crossOrder_twoTickets_onlyOneSuccess_noDeadlock() throws InterruptedException {
        // given
        Member seller = memberFixture.create();
        Member buyer1 = memberFixture.create();
        Member buyer2 = memberFixture.create();

        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket tA = ticketFixture.create(seller, session, 80);
        Ticket tB = ticketFixture.create(seller, session, 80);

        Long aId = tA.getId();
        Long bId = tB.getId();

        // 서로 반대 순서(교차)
        List<Long> orderAB = List.of(aId, bId);
        List<Long> orderBA = List.of(bId, aId);

        ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);

        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(2);

        java.util.concurrent.atomic.AtomicReference<BookingService.HoldResult> r1 = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<BookingService.HoldResult> r2 = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> e1 = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> e2 = new java.util.concurrent.atomic.AtomicReference<>();

        Runnable task1 = () -> {
            ready.countDown();
            try {
                start.await();
                r1.set(bookingService.holdTickets(buyer1.getId(), session.getId(), orderAB));
            } catch (Throwable t) {
                e1.set(t);
            } finally {
                done.countDown();
            }
        };

        Runnable task2 = () -> {
            ready.countDown();
            try {
                start.await();
                r2.set(bookingService.holdTickets(buyer2.getId(), session.getId(), orderBA));
            } catch (Throwable t) {
                e2.set(t);
            } finally {
                done.countDown();
            }
        };

        pool.submit(task1);
        pool.submit(task2);

        // 둘 다 준비될 때까지 대기
        assertThat(ready.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        // when (동시에 출발)
        start.countDown();

        // then (데드락이면 여기서 timeout으로 걸리는 게 흔함)
        assertThat(done.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        BookingService.HoldResult success = null;
        Throwable failure = null;

        if (r1.get() != null) success = r1.get();
        if (r2.get() != null) success = r2.get();

        if (e1.get() != null) failure = e1.get();
        if (e2.get() != null) failure = e2.get();

        // 성공은 1건, 실패는 1건이어야 함
        assertThat(success).isNotNull();
        assertThat(failure).isNotNull();

        // 실패는 보통 "이미 다른 사용자가 선점" 류
        if (failure instanceof MoaException me) {
            assertThat(me.getType()).isIn(MoaExceptionType.TICKET_ALREADY_HELD);
        } else {
            throw new AssertionError("Unexpected exception type", failure);
        }

        // DB 검증: A,B 둘 다 HOLD, holdToken은 성공 토큰과 동일
        Ticket reloadedA = ticketFixture.findById(aId);
        Ticket reloadedB = ticketFixture.findById(bId);

        org.assertj.core.api.Assertions.assertThat(reloadedA.getState()).isEqualTo(TicketState.HOLD);
        org.assertj.core.api.Assertions.assertThat(reloadedB.getState()).isEqualTo(TicketState.HOLD);

        org.assertj.core.api.Assertions.assertThat(reloadedA.getHoldToken()).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(reloadedB.getHoldToken()).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(reloadedA.getHoldToken()).isEqualTo(success.holdToken());
        org.assertj.core.api.Assertions.assertThat(reloadedB.getHoldToken()).isEqualTo(success.holdToken());

        // 소유자도 성공한 사람이어야 함 (buyer1 or buyer2 중 하나)
        Long ownerIdA = reloadedA.getMember().getId();
        Long ownerIdB = reloadedB.getMember().getId();

        org.assertj.core.api.Assertions.assertThat(ownerIdA).isEqualTo(ownerIdB);
        org.assertj.core.api.Assertions.assertThat(ownerIdA).isIn(buyer1.getId(), buyer2.getId());
    }

    @Test
    @DisplayName("[atomic] 2장 중 1장이 이미 HOLD면 전체 HOLD 실패하고 어떤 티켓도 변경되지 않는다.")
    void holdTickets_atomic_failWhenOneAlreadyHold_noPartialUpdate() {
        // given
        Member seller = memberFixture.create();
        Member buyer1 = memberFixture.create();
        Member buyer2 = memberFixture.create();

        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket t1 = ticketFixture.create(seller, session, 80);
        Ticket t2 = ticketFixture.create(seller, session, 80);

        // buyer2가 t2를 먼저 hold
        BookingService.HoldResult otherHold = bookingService.holdTickets(
                buyer2.getId(), session.getId(), List.of(t2.getId())
        );

        // when & then (buyer1이 t1+t2 같이 잡으려다 실패해야 함)
        assertThatThrownBy(() -> bookingService.holdTickets(
                buyer1.getId(), session.getId(), List.of(t1.getId(), t2.getId())
        ))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.TICKET_ALREADY_HELD);

        // then: t1은 절대 hold되면 안 됨 (부분 성공 불허)
        Ticket reloaded1 = ticketFixture.findById(t1.getId());
        Ticket reloaded2 = ticketFixture.findById(t2.getId());

        assertThat(reloaded1.getState()).isEqualTo(TicketState.AVAILABLE); // 핵심!
        assertThat(reloaded1.getHoldToken()).isNull();
        assertThat(reloaded1.getExpiresAt()).isNull();

        assertThat(reloaded2.getState()).isEqualTo(TicketState.HOLD);
        assertThat(reloaded2.getHoldToken()).isEqualTo(otherHold.holdToken());
        assertThat(reloaded2.getMember().getId()).isEqualTo(buyer2.getId());
    }

    @Test
    @DisplayName("[concurrency][atomic] [A,B] vs [B,C] 동시 hold면 하나의 요청만 전체 성공하고 다른 요청은 전체 실패(부분 성공 없음).")
    void holdTickets_concurrent_overlapRequests_onlyOneRequestFullySucceeds() throws Exception {
        // given
        Member seller = memberFixture.create();
        Member buyer1 = memberFixture.create();
        Member buyer2 = memberFixture.create();

        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket A = ticketFixture.create(seller, session, 80);
        Ticket B = ticketFixture.create(seller, session, 80);
        Ticket C = ticketFixture.create(seller, session, 80);

        List<Long> req1 = List.of(A.getId(), B.getId()); // [A,B]
        List<Long> req2 = List.of(B.getId(), C.getId()); // [B,C]

        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicReference<BookingService.HoldResult> r1 = new AtomicReference<>();
        AtomicReference<BookingService.HoldResult> r2 = new AtomicReference<>();
        AtomicReference<Throwable> e1 = new AtomicReference<>();
        AtomicReference<Throwable> e2 = new AtomicReference<>();

        Runnable task1 = () -> {
            ready.countDown();
            try {
                start.await();
                r1.set(bookingService.holdTickets(buyer1.getId(), session.getId(), req1));
            } catch (Throwable t) {
                e1.set(t);
            } finally {
                done.countDown();
            }
        };

        Runnable task2 = () -> {
            ready.countDown();
            try {
                start.await();
                r2.set(bookingService.holdTickets(buyer2.getId(), session.getId(), req2));
            } catch (Throwable t) {
                e2.set(t);
            } finally {
                done.countDown();
            }
        };

        pool.submit(task1);
        pool.submit(task2);

        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();

        // when
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        // then: 성공 1개, 실패 1개
        int success = 0;
        if (r1.get() != null) success++;
        if (r2.get() != null) success++;
        assertThat(success).isEqualTo(1);

        // 실패는 TICKET_ALREADY_HELD 여야 함
        Throwable failure = (r1.get() == null) ? e1.get() : e2.get();
        assertThat(failure).isInstanceOf(MoaException.class);
        assertThat(((MoaException) failure).getType()).isEqualTo(MoaExceptionType.TICKET_ALREADY_HELD);

        // 핵심: 부분 성공이 없어야 함
        // - 승리한 요청은 2장이 모두 HOLD
        // - 패배한 요청은 2장 중 "자기 요청에 포함된 다른 1장"이 홀드되면 안 됨 (즉, 0장 성공이어야 함)
        // 결과적으로 A,B,C 중 HOLD는 정확히 2장만 존재해야 함
        Ticket a = ticketFixture.findById(A.getId());
        Ticket b = ticketFixture.findById(B.getId());
        Ticket c = ticketFixture.findById(C.getId());

        long holdCount = List.of(a,b,c).stream().filter(t -> t.getState() == TicketState.HOLD).count();
        assertThat(holdCount).isEqualTo(2);

        boolean req1Won = a.getState() == TicketState.HOLD && b.getState() == TicketState.HOLD && c.getState() == TicketState.AVAILABLE;
        boolean req2Won = b.getState() == TicketState.HOLD && c.getState() == TicketState.HOLD && a.getState() == TicketState.AVAILABLE;
        assertThat(req1Won || req2Won).isTrue();

        // 어떤 holdToken이든, "홀드된 두 장의 holdToken은 동일해야 함" (한 요청의 결과니까)
        List<Ticket> held = List.of(a,b,c).stream().filter(t -> t.getState() == TicketState.HOLD).toList();
        assertThat(held.get(0).getHoldToken()).isNotBlank();
        assertThat(held.get(0).getHoldToken()).isEqualTo(held.get(1).getHoldToken());

        String winnerToken = (r1.get() != null) ? r1.get().holdToken() : r2.get().holdToken();
        assertThat(held.get(0).getHoldToken()).isEqualTo(winnerToken);

    }

    @Test
    @DisplayName("[idempotency] 동일 member가 동일 ticketIds를 재요청하면 새 holdToken을 발급하지 않고 기존 holdToken/expiresAt을 그대로 반환한다.")
    void holdTickets_idempotent_sameMemberSameTickets_returnsSameTokenAndExpiresAt() {
        // given
        Member seller = memberFixture.create();
        Member buyer = memberFixture.create();

        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket t1 = ticketFixture.create(seller, session, 80);
        Ticket t2 = ticketFixture.create(seller, session, 80);

        List<Long> ticketIds = List.of(t1.getId(), t2.getId());

        // when (1차 hold)
        BookingService.HoldResult first = bookingService.holdTickets(buyer.getId(), session.getId(), ticketIds);

        // when (2차 hold - 재요청)
        BookingService.HoldResult second = bookingService.holdTickets(buyer.getId(), session.getId(), ticketIds);

        // then: 멱등 성공(토큰/만료 동일)
        assertThat(first.holdToken()).isNotBlank();
        assertThat(first.expiresAt()).isNotNull();

        assertThat(second.holdToken()).isEqualTo(first.holdToken());
        assertThat(second.expiresAt()).isEqualTo(first.expiresAt());

        // then: DB 반영도 그대로 유지
        // holdToken으로 조회해서 2장인지 확인
        List<Ticket> held = ticketRepositoryQueryDsl.findTicketsByHoldToken(first.holdToken());
        assertThat(held).hasSize(2);
        assertThat(held).allMatch(Ticket::isHold);
        assertThat(held).allMatch(t -> first.holdToken().equals(t.getHoldToken()));
        assertThat(held).allMatch(t -> first.expiresAt().equals(t.getExpiresAt()));
    }

    @Test
    @DisplayName("[idempotency] 동일 member가 ticketIds를 변경해 재요청하면 멱등 처리되지 않고 409(TICKET_ALREADY_HELD)로 실패하며 기존 HOLD는 유지된다.")
    void holdTickets_idempotent_partialChange_failsAndKeepsExistingHold() {
        // given
        Member seller = memberFixture.create();
        Member buyer = memberFixture.create();

        Hall hall = hallFixture.create();
        Concert concert = concertFixture.create(seller, hall);
        Session session = sessionFixture.create(concert);

        Ticket t1 = ticketFixture.create(seller, session, 80);
        Ticket t2 = ticketFixture.create(seller, session, 80);
        Ticket t3 = ticketFixture.create(seller, session, 80);

        List<Long> firstIds = List.of(t1.getId(), t2.getId());
        List<Long> secondIds = List.of(t1.getId(), t2.getId(), t3.getId());

        // when (1차 hold)
        BookingService.HoldResult first = bookingService.holdTickets(buyer.getId(), session.getId(), firstIds);

        // when&then (2차 hold - 재요청)
        assertThatThrownBy(() -> bookingService.holdTickets(buyer.getId(), session.getId(), secondIds))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.TICKET_ALREADY_HELD);

        // then: 기존 HOLD(1,2)는 유지
        List<Ticket> held = ticketRepositoryQueryDsl.findTicketsByHoldToken(first.holdToken());
        assertThat(held).hasSize(2);
        assertThat(held).allMatch(Ticket::isHold);
        assertThat(held).allMatch(t -> first.holdToken().equals(t.getHoldToken()));
        assertThat(held).allMatch(t -> first.expiresAt().equals(t.getExpiresAt()));

        // 티켓3은 그대로 AVAILABLE
        Ticket t3Reloaded = ticketFixture.findById(t3.getId());
        assertThat(t3Reloaded.getState()).isEqualTo(TicketState.AVAILABLE);
        assertThat(t3Reloaded.getHoldToken()).isNull();
        assertThat(t3Reloaded.getExpiresAt()).isNull();

    }

}
