package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.alarm.core.component.AlarmMessageFactory;
import stack.moaticket.system.alarm.core.model.AlarmMessage;
import stack.moaticket.system.alarm.core.service.AlarmSendService;
import stack.moaticket.domain.session_start_alarm.dto.SessionStartAlarmMetaDto;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.domain.ticket.dto.TicketMetaDto;
import stack.moaticket.domain.ticket.entity.Ticket;
import stack.moaticket.domain.ticket.service.TicketService;
import stack.moaticket.domain.ticket_alarm.service.TicketAlarmService;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.alarm.sse.service.SseSubscribeService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {
    private final Validator validator;

    private final AlarmSendService alarmSendService;

    private final SseSubscribeService sseSubscribeService;

    private final MemberService memberService;
    private final TicketService ticketService;
    private final TicketAlarmService ticketAlarmService;

    public SseEmitter subscribe(Long memberId) {
        validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED);

        return sseSubscribeService.subscribe(memberId);
    }

    @Transactional
    public void subscribeTicketReleaseAlarm(Long memberId, Long ticketId) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Ticket ticket = validator.of(ticketService.get(ticketId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.TICKET_NOT_FOUND)
                .get();

        ticketAlarmService.createAndSave(member, ticket);
    }

    public void sendConcertStartInform(List<SessionStartAlarmMetaDto> alarmMetadata) {
        for(SessionStartAlarmMetaDto alarm : alarmMetadata) {
            Long memberId = alarm.memberId();
            AlarmMessage message = AlarmMessageFactory.sessionStart(alarm);
            alarmSendService.sendAll(memberId, message);
        }
    }

    // 홀드 풀릴떄 보내는 알림
    public void sendTicketReleaseInform(Map<Long, List<Long>> receiverMap, Map<Long, TicketMetaDto> ticketMetadata) {
        // 보내야할 유저가 많으면 키가 많아지고 루프가 많이 실행됨 o(n) 속도 체크 필요하다
        for(Map.Entry<Long, List<Long>> entry : receiverMap.entrySet()) {
            Long memberId = entry.getKey();

            List<Long> ticketIdList = entry.getValue();
            if(ticketIdList == null || ticketIdList.isEmpty()) continue;

            List<TicketMetaDto> metaList = ticketIdList.stream()
                    .map(ticketMetadata::get)
                    .filter(Objects::nonNull)
                    .toList();
            if(metaList.isEmpty()) continue;

            AlarmMessage message = AlarmMessageFactory.ticketRelease(metaList);
            alarmSendService.sendAll(memberId,message);
        }
    }
}
