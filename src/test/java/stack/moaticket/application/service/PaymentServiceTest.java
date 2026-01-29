package stack.moaticket.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import stack.moaticket.application.dto.PaymentDto;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.domain.payment.entity.Payment;
import stack.moaticket.domain.payment.repository.PaymentRepository;
import stack.moaticket.domain.payment.type.PaymentState;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.repository.TicketRepositoryQueryDsl;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaException;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.util.TokenGenerator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    // 실제 객체
    private final Validator validator = new Validator();

    @Mock
    private MemberService memberService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TicketRepositoryQueryDsl ticketRepositoryQueryDsl;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                validator,
                memberService,
                paymentRepository,
                ticketRepositoryQueryDsl
        );
    }

    @Test
    @DisplayName("회원이 존재하지 않으면 MEMBER_NOT_FOUND 예외가 발생한다.")
    void prepare_memberNotFound_throwsMemberNotFound() {
        // given
        Long memberId = 1L;
        PaymentDto.PrepareRequest request = PaymentDto.PrepareRequest.builder()
                .holdToken("hold_123")
                .build();

        given(memberService.findById(memberId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> paymentService.prepare(memberId, request))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MEMBER_NOT_FOUND);

        then(ticketRepositoryQueryDsl).shouldHaveNoInteractions();
        then(paymentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("회원 상태가 ACTIVE가 아니면 UNAUTHORIZED 예외가 발생한다.")
    void prepare_memberNotActive_throwsUnauthorized() {
        // given
        Long memberId = 1L;
        PaymentDto.PrepareRequest request = PaymentDto.PrepareRequest.builder()
                .holdToken("hold_123")
                .build();

        Member member = mock(Member.class);
        given(member.getState()).willReturn(MemberState.BLOCKED);
        given(memberService.findById(memberId)).willReturn(member);

        // when & then
        assertThatThrownBy(() -> paymentService.prepare(memberId, request))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.UNAUTHORIZED);

        then(ticketRepositoryQueryDsl).shouldHaveNoInteractions();
        then(paymentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("request가 null이면 MISMATCH_PARAMETER 예외가 발생한다.")
    void prepare_requestNull_throwsMismatchParameter() {
        // given
        Long memberId = 1L;
        givenActiveMember_stateOnly(memberId);

        // when & then
        assertThatThrownBy(() -> paymentService.prepare(memberId, null))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MISMATCH_PARAMETER);

        then(ticketRepositoryQueryDsl).shouldHaveNoInteractions();
        then(paymentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("holdToken이 null이면 MISMATCH_PARAMETER 예외가 발생한다.")
    void prepare_holdTokenNull_throwsMismatchParameter() {
        // given
        Long memberId = 1L;
        givenActiveMember_stateOnly(memberId);

        PaymentDto.PrepareRequest request = PaymentDto.PrepareRequest.builder()
                .holdToken(null)
                .build();

        // when & then
        assertThatThrownBy(() -> paymentService.prepare(memberId, request))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MISMATCH_PARAMETER);

        then(ticketRepositoryQueryDsl).shouldHaveNoInteractions();
        then(paymentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("holdToken이 blank면 MISMATCH_PARAMETER 예외가 발생한다.")
    void prepare_holdTokenBlank_throwsMismatchParameter() {
        // given
        Long memberId = 1L;
        givenActiveMember_stateOnly(memberId);

        PaymentDto.PrepareRequest request = PaymentDto.PrepareRequest.builder()
                .holdToken("   ")
                .build();

        // when & then
        assertThatThrownBy(() -> paymentService.prepare(memberId, request))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.MISMATCH_PARAMETER);

        then(ticketRepositoryQueryDsl).shouldHaveNoInteractions();
        then(paymentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("holdToken으로 조회된 티켓이 없으면 HOLD_EXPIRED 예외가 발생한다.")
    void prepare_ticketsEmpty_throwsHoldExpired() {
        // given
        Long memberId = 1L;
        givenActiveMember_stateOnly(memberId);

        PaymentDto.PrepareRequest request = PaymentDto.PrepareRequest.builder()
                .holdToken("hold_123")
                .build();

        given(ticketRepositoryQueryDsl.findTicketsByHoldToken("hold_123"))
                .willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> paymentService.prepare(memberId, request))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.HOLD_EXPIRED);

        then(ticketRepositoryQueryDsl).should().findTicketsByHoldToken("hold_123");
        then(paymentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("티켓 소유자가 아니면 FORBIDDEN 예외가 발생한다.")
    void prepare_ownerMismatch_throwsForbidden() {
        // given
        Long memberId = 1L;
        Member member = givenActiveMember(memberId);

        PaymentDto.PrepareRequest request = PaymentDto.PrepareRequest.builder()
                .holdToken("hold_123")
                .build();

        Ticket t1 = mock(Ticket.class);
        given(t1.isOwnedBy(member.getId())).willReturn(false);

        given(ticketRepositoryQueryDsl.findTicketsByHoldToken("hold_123"))
                .willReturn(List.of(t1));

        // when & then
        assertThatThrownBy(() -> paymentService.prepare(memberId, request))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.FORBIDDEN);

        then(paymentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("티켓 hold가 만료/무효면 HOLD_EXPIRED 예외가 발생한다.")
    void prepare_holdInvalid_throwsHoldExpired() {
        // given
        Long memberId = 1L;
        Member member = givenActiveMember(memberId);

        PaymentDto.PrepareRequest request = PaymentDto.PrepareRequest.builder()
                .holdToken("hold_123")
                .build();

        Ticket t1 = mock(Ticket.class);
        given(t1.isOwnedBy(member.getId())).willReturn(true);

        // any(LocalDateTime.class) 인자를 받는 메서드라서 any()로 처리
        given(t1.isHoldValidAt(any())).willReturn(false);

        given(ticketRepositoryQueryDsl.findTicketsByHoldToken("hold_123"))
                .willReturn(List.of(t1));

        // when & then
        assertThatThrownBy(() -> paymentService.prepare(memberId, request))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.HOLD_EXPIRED);

        then(paymentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("티켓들이 같은 session이 아니면 VALIDATION_FAILED 예외가 발생한다.")
    void prepare_notAllSameSession_throwsValidationFailed() {
        // given
        Long memberId = 1L;
        Member member = givenActiveMember(memberId);

        PaymentDto.PrepareRequest request = PaymentDto.PrepareRequest.builder()
                .holdToken("hold_123")
                .build();

        Ticket t1 = mock(Ticket.class);
        Ticket t2 = mock(Ticket.class);

        // owner/hold는 통과
        given(t1.isOwnedBy(member.getId())).willReturn(true);
        given(t2.isOwnedBy(member.getId())).willReturn(true);
        given(t1.isHoldValidAt(any())).willReturn(true);
        given(t2.isHoldValidAt(any())).willReturn(true);

        // 세션 단일성 실패 유도:
        // - 첫 티켓이 "sessionId=10"이라고 치면,
        // - 두 번째 티켓은 그 sessionId와 다르다고 반환
        stack.moaticket.domain.session.entity.Session s1 = mock(stack.moaticket.domain.session.entity.Session.class);
        given(s1.getId()).willReturn(10L);
        given(t1.getSession()).willReturn(s1);

        given(t1.isSameSession(10L)).willReturn(true);
        given(t2.isSameSession(10L)).willReturn(false);

        given(ticketRepositoryQueryDsl.findTicketsByHoldToken("hold_123"))
                .willReturn(List.of(t1, t2));

        // when & then
        assertThatThrownBy(() -> paymentService.prepare(memberId, request))
                .isInstanceOf(MoaException.class)
                .extracting(e -> ((MoaException) e).getType())
                .isEqualTo(MoaExceptionType.VALIDATION_FAILED);

        then(paymentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("정상 요청이면 amount/orderName/orderId를 생성하고 Payment(READY)를 저장한 뒤 PrepareResponse를 반환한다.")
    void prepare_success_savesReadyPayment_andReturnsPrepareResponse() {
        // given
        Long memberId = 1L;
        Member member = givenActiveMember(memberId);

        PaymentDto.PrepareRequest request = PaymentDto.PrepareRequest.builder()
                .holdToken("hold_123")
                .build();

        // Ticket 2개, 가격 5000 + 7000 = 12000
        Ticket t1 = mock(Ticket.class);
        Ticket t2 = mock(Ticket.class);

        given(t1.isOwnedBy(member.getId())).willReturn(true);
        given(t2.isOwnedBy(member.getId())).willReturn(true);
        given(t1.isHoldValidAt(any())).willReturn(true);
        given(t2.isHoldValidAt(any())).willReturn(true);

        // session / price / concert name 세팅
        stack.moaticket.domain.concert.entity.Concert concert = mock(stack.moaticket.domain.concert.entity.Concert.class);
        given(concert.getName()).willReturn("콘서트A");

        stack.moaticket.domain.session.entity.Session session = mock(stack.moaticket.domain.session.entity.Session.class);
        given(session.getId()).willReturn(10L);
        given(session.getPrice()).willReturn(5000);
        given(session.getConcert()).willReturn(concert);

        stack.moaticket.domain.session.entity.Session session2 = mock(stack.moaticket.domain.session.entity.Session.class);
        given(session2.getPrice()).willReturn(7000);

        given(t1.getSession()).willReturn(session);
        given(t2.getSession()).willReturn(session2);

        // 세션 단일성(둘 다 10L이라고 가정)
        given(t1.isSameSession(10L)).willReturn(true);
        given(t2.isSameSession(10L)).willReturn(true);

        given(ticketRepositoryQueryDsl.findTicketsByHoldToken("hold_123"))
                .willReturn(List.of(t1, t2));

        // static orderId 고정
        try (MockedStatic<TokenGenerator> mocked = mockStatic(TokenGenerator.class)) {
            mocked.when(TokenGenerator::generateOrderId).thenReturn("order_fixed_123");

            // when
            PaymentDto.PrepareResponse response = paymentService.prepare(memberId, request);

            // then
            assertThat(response.getOrderId()).isEqualTo("order_fixed_123");
            assertThat(response.getOrderName()).isEqualTo("콘서트A 2매");
            assertThat(response.getAmount()).isEqualTo(12000);

            // Payment 저장 검증 (ArgumentCaptor로 값 검증)
            then(paymentRepository).should().save(argThat(p ->
                    p.getMember() == member
                            && p.getOrderId().equals("order_fixed_123")
                            && p.getOrderName().equals("콘서트A 2매")
                            && p.getHoldToken().equals("hold_123")
                            && p.getAmount() == 12000
                            && p.getState() == PaymentState.READY
            ));
        }
    }

    // Helpers
    private Member givenActiveMember(Long memberId) {
        Member member = mock(Member.class);
        given(member.getState()).willReturn(MemberState.ACTIVE);
        given(member.getId()).willReturn(memberId);
        given(memberService.findById(memberId)).willReturn(member);
        return member;
    }

    private Member givenActiveMember_stateOnly(Long memberId) {
        Member member = mock(Member.class);
        given(member.getState()).willReturn(MemberState.ACTIVE);
        given(memberService.findById(memberId)).willReturn(member);
        return member;
    }

}
