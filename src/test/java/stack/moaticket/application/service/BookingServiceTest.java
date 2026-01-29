package stack.moaticket.application.service;

//JUnit5
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

//Mockito (JUnit5 확장 + 어노테이션)
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;


//AssertJ (예외 검증)
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

//Mockito BDD 스타일(stubbing)
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyList;
import static org.mockito.BDDMockito.anyLong;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.never;

//SUT/의존성 클래스
import stack.moaticket.application.dto.BookingDto;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepositoryQueryDsl;
import stack.moaticket.domain.ticket.type.TicketState;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;

//컬렉션
import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    // 실제 객체
    private final Validator validator = new Validator();

    @Mock
    private MemberService memberService;

    @Mock
    private TicketRepositoryQueryDsl ticketRepositoryQueryDsl;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(validator, memberService, ticketRepositoryQueryDsl);
    }

    @Test
    @DisplayName("회원이 존재하지 않으면 MEMBER_NOT_FOUND 예외가 발생한다.")
    void holdTickets_memberNotFound_throwsMemberNotFound() {
        // given
        Long memberId = 1L;
        Long sessionId = 10L;
        List<Long> ticketIds = List.of(1L);

        given(memberService.findById(memberId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> bookingService.holdTickets(memberId, sessionId, ticketIds))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("회원 상태가 ACTIVE가 아니면 UNAUTHORIZED 예외가 발생한다.")
    void holdTickets_memberNotActive_throwsUnauthorized() {
        // given
        Long memberId = 1L;
        Long sessionId = 10L;
        List<Long> ticketIds = List.of(1L);

        Member member = mock(Member.class);
        given(member.getState()).willReturn(MemberState.BLOCKED);
        given(memberService.findById(memberId)).willReturn(member);

        // when & then
        assertThatThrownBy(() -> bookingService.holdTickets(memberId, sessionId, ticketIds))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.UNAUTHORIZED);
    }

    @Test
    @DisplayName("회차 ID가 null이면 VALIDATION_FAILED 예외가 발생한다.")
    void holdTickets_sessionIdNull_throwsValidationFailed() {
        // given
        Long memberId = 1L;
        Long sessionId = null;
        List<Long> ticketIds = List.of(1L);

        givenActiveMember(memberId);

        // when & then
        assertThatThrownBy(() -> bookingService.holdTickets(memberId, sessionId, ticketIds))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("티켓 ID 목록이 null이면 VALIDATION_FAILED 예외가 발생한다.")
    void holdTickets_ticketIdsNull_throwsValidationFailed() {
        // given
        Long memberId = 1L;
        Long sessionId = 10L;
        List<Long> ticketIds = null;

        givenActiveMember(memberId);

        // when & then
        assertThatThrownBy(() -> bookingService.holdTickets(memberId, sessionId, ticketIds))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("티켓 ID 목록이 비어있으면 VALIDATION_FAILED 예외가 발생한다.")
    void holdTickets_ticketIdsEmpty_throwsValidationFailed() {
        // given
        Long memberId = 1L;
        Long sessionId = 10L;
        List<Long> ticketIds = List.of();

        givenActiveMember(memberId);

        // when & then
        assertThatThrownBy(() -> bookingService.holdTickets(memberId, sessionId, ticketIds))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("티켓 ID가 4개를 초과하면 VALIDATION_FAILED 예외가 발생한다.")
    void holdTickets_ticketIdsOverLimit_throwsValidationFailed() {
        // given
        Long memberId = 1L;
        Long sessionId = 10L;
        List<Long> ticketIds = List.of(1L, 2L, 3L, 4L, 5L);

        givenActiveMember(memberId);

        // when & then
        assertThatThrownBy(() -> bookingService.holdTickets(memberId, sessionId, ticketIds))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("중복된 티켓 ID가 포함되면 VALIDATION_FAILED 예외가 발생한다.")
    void holdTickets_duplicateTicketIds_throwsValidationFailed() {
        // given
        Long memberId = 1L;
        Long sessionId = 10L;
        List<Long> ticketIds = List.of(1L, 1L);

        givenActiveMember(memberId);

        // when & then
        assertThatThrownBy(() -> bookingService.holdTickets(memberId, sessionId, ticketIds))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.VALIDATION_FAILED);
    }


    @Test
    @DisplayName("이미 구매한 수 + 요청 수가 4를 초과하면 TICKET_LIMIT_EXCEEDED 예외가 발생한다.")
    void holdTickets_ticketLimitExceeded_throwsTicketLimitExceeded() {
        // given
        Long memberId = 1L;
        Long sessionId = 10L;
        List<Long> ticketIds = List.of(1L, 2L); // 요청 2장

        givenActiveMember(memberId);

        // 이미 3장 구매했다고 치면 3 + 2 > 4
        given(ticketRepositoryQueryDsl.countSoldByMemberAndSession(memberId, sessionId))
                .willReturn(3L);

        // when & then
        assertThatThrownBy(() -> bookingService.holdTickets(memberId, sessionId, ticketIds))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.TICKET_LIMIT_EXCEEDED);

        // quota에서 끊기므로 티켓 조회는 안 해야 함
        then(ticketRepositoryQueryDsl).should(never()).findTicketsForUpdate(anyList(), anyLong());
    }

    @Test
    @DisplayName("요청한 티켓 수만큼 조회되지 않으면 MISMATCH_PARAMETER 예외가 발생한다.")
    void holdTickets_ticketsSizeMismatch_throwsMismatchParameter() {
        // given
        Long memberId = 1L;
        Long sessionId = 10L;
        List<Long> ticketIds = List.of(2L, 1L); // 일부러 섞어서(정렬 확인에도 도움)

        givenActiveMember(memberId);

        given(ticketRepositoryQueryDsl.countSoldByMemberAndSession(memberId, sessionId))
                .willReturn(0L);

        // 정렬된 ID로 조회될 것: [1,2]
        Ticket t1 = mock(Ticket.class);

        // 2개 요청했는데 1개만 반환 → mismatch
        given(ticketRepositoryQueryDsl.findTicketsForUpdate(List.of(1L, 2L), sessionId))
                .willReturn(List.of(t1));

        // when & then
        assertThatThrownBy(() -> bookingService.holdTickets(memberId, sessionId, ticketIds))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MISMATCH_PARAMETER);
    }

    @Test
    @DisplayName("조회된 티켓 중 AVAILABLE이 아닌 것이 있으면 TICKET_ALREADY_HELD 예외가 발생한다.")
    void holdTickets_notAllAvailable_throwsTicketAlreadyHeld() {
        // given
        Long memberId = 1L;
        Long sessionId = 10L;
        List<Long> ticketIds = List.of(1L, 2L);

        givenActiveMember(memberId);

        given(ticketRepositoryQueryDsl.countSoldByMemberAndSession(memberId, sessionId))
                .willReturn(0L);

        Ticket t1 = mock(Ticket.class);
        Ticket t2 = mock(Ticket.class);

        // 하나라도 false면 allAvailable = false
        given(t1.isAvailable()).willReturn(true);
        given(t2.isAvailable()).willReturn(false);

        given(ticketRepositoryQueryDsl.findTicketsForUpdate(List.of(1L, 2L), sessionId))
                .willReturn(List.of(t1, t2));

        // when & then
        assertThatThrownBy(() -> bookingService.holdTickets(memberId, sessionId, ticketIds))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.TICKET_ALREADY_HELD);
    }

    @Test
    @DisplayName("정상 요청이면 티켓들을 hold 처리하고 holdToken과 expiresAt을 반환한다.")
    void holdTickets_success_returnsHoldResultAndCallsHoldBy() {
        // given
        Long memberId = 1L;
        Long sessionId = 10L;
        List<Long> ticketIds = List.of(3L, 1L, 2L);

        Member member = givenActiveMember(memberId);

        given(ticketRepositoryQueryDsl.countSoldByMemberAndSession(memberId, sessionId))
                .willReturn(0L);

        Ticket t1 = mock(Ticket.class);
        Ticket t2 = mock(Ticket.class);
        Ticket t3 = mock(Ticket.class);

        given(t1.isAvailable()).willReturn(true);
        given(t2.isAvailable()).willReturn(true);
        given(t3.isAvailable()).willReturn(true);

        given(ticketRepositoryQueryDsl.findTicketsForUpdate(List.of(1L, 2L, 3L), sessionId))
                .willReturn(List.of(t1, t2, t3));

        // when
        BookingService.HoldResult result = bookingService.holdTickets(memberId, sessionId, ticketIds);

        // then
        // 토큰/만료시간은 랜덤/시간이므로 존재만 확인
        org.assertj.core.api.Assertions.assertThat(result.holdToken()).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(result.expiresAt()).isNotNull();

        then(t1).should().holdBy(eq(member), anyString(), any(LocalDateTime.class));
        then(t2).should().holdBy(eq(member), anyString(), any(LocalDateTime.class));
        then(t3).should().holdBy(eq(member), anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("회차 ID가 null이면 VALIDATION_FAILED 예외가 발생한다.")
    void getTicketsBySession_sessionIdNull_throwsValidationFailed() {
        // when & then
        assertThatThrownBy(() -> bookingService.getTicketsBySession(null))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("회차 좌석 목록을 조회해 TicketResponse로 매핑해 반환한다.")
    void getTicketsBySession_returnsMappedTicketResponses() {
        // given
        Long sessionId = 10L;

        // viewState() 실제 메서드 실행을 위해 spy로 부름. 안 쓴다면 TicketState로 넣어야함
        Ticket available = spyTicket(1L, 1, true, false);
        Ticket hold = spyTicket(2L, 2, false, true);
        Ticket sold = spyTicket(3L, 3, false, false);

        given(ticketRepositoryQueryDsl.getTicketsBySession(sessionId))
                .willReturn(List.of(available, hold, sold));

        // when
        List<BookingDto.TicketResponse> result =
                bookingService.getTicketsBySession(sessionId);

        // then
        assertThat(result).hasSize(3);

        assertThat(result.getFirst().getTicketId()).isEqualTo(1L);
        assertThat(result.getFirst().getSeatNum()).isEqualTo(1);
        assertThat(result.getFirst().getState()).isEqualTo(TicketState.AVAILABLE.name());

        assertThat(result.get(1).getTicketId()).isEqualTo(2L);
        assertThat(result.get(1).getSeatNum()).isEqualTo(2);
        assertThat(result.get(1).getState()).isEqualTo(TicketState.HOLD.name());

        assertThat(result.get(2).getTicketId()).isEqualTo(3L);
        assertThat(result.get(2).getSeatNum()).isEqualTo(3);
        assertThat(result.get(2).getState()).isEqualTo(TicketState.SOLD.name());

    }


    // Helpers

    // Active Member
    private Member givenActiveMember(Long memberId) {
        Member member = mock(Member.class);
        given(member.getState()).willReturn(MemberState.ACTIVE);
        given(memberService.findById(memberId)).willReturn(member);
        return member;
    }

    // Ticket spy 생성 + 상태 설정
    private Ticket spyTicket(long id, int seatNum, boolean available, boolean hold) {
        // viewState() 실제 메서드 실행을 위해 spy로 부름. 안 쓴다면 TicketState로 넣어야함
        Ticket t = spy(Ticket.class);

        // STRICT 모드에서 stub하고 안 쓴 stubbling은 에러 -> given을 쓰면 실제 메서드를 호출하려고 시도함 -> UnnecessaryStubbingException
        // doReturn은 메서드 호출 없이 바로 stub
        // spy - doReturn()
        doReturn(id).when(t).getId();
        doReturn(seatNum).when(t).getNum();
        doReturn(available).when(t).isAvailable();

        // viewState() 분기 때문에 available=true면 isHold는 불필요 stub이 될 수 있음
        if (!available) {
            doReturn(hold).when(t).isHold();
        }

        return t;
    }

}
