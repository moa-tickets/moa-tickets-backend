package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.system.alarm.core.util.AlarmMessageFactory;
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
import stack.moaticket.system.alarm.core.util.AlarmShardUtil;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaExceptionType;
import stack.moaticket.system.alarm.sse.service.SseSubscribeService;

import java.util.*;

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

    @Value("${app.server.alarm.shard-count}")
    private int shardCount;

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

    @Transactional
    public void unsubscribeTicketReleaseAlarm(Long memberId, Long ticketId) {
        Member member = validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED)
                .get();

        Ticket ticket = validator.of(ticketService.get(ticketId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.TICKET_NOT_FOUND)
                .get();

        ticketAlarmService.delete(member, ticket);
    }

    public void sendConcertStartInform(List<SessionStartAlarmMetaDto> alarmMetadata) {
        final int cutoff = 200;
        Map<Integer, List<SessionStartAlarmMetaDto>> shardMap = AlarmShardUtil.createShardMap(shardCount);

        for(SessionStartAlarmMetaDto alarm : alarmMetadata) {
            int shardNum = AlarmShardUtil.getShardNum(alarm.memberId(), shardCount);

            shardMap.get(shardNum).add(alarm);
        }

        alarmSendService.sendToShards(
                shardMap,
                t -> {
                    Long memberId = t.memberId();
                    AlarmMessage message = AlarmMessageFactory.sessionStart(t);

                    alarmSendService.sendAll(memberId, message);
                },
                cutoff);
    }

    // 홀드 풀릴떄 보내는 알림
    public void sendTicketReleaseInform(Map<Long, List<Long>> receiverMap, Map<Long, TicketMetaDto> ticketMetadata) {
        Map<Integer, List<TicketReleaseSendData>> shardMap = AlarmShardUtil.createShardMap(shardCount);
        final int cutoff = 200;

        // 보내야할 유저가 많으면 키가 많아지고 루프가 많이 실행됨 o(n) 속도 체크 필요하다
        for(Map.Entry<Long, List<Long>> entry : receiverMap.entrySet()) {
            Long memberId = entry.getKey();

            List<Long> ticketIdList = entry.getValue();
            if(ticketIdList == null || ticketIdList.isEmpty()) continue;

            List<TicketMetaDto> metaList = new ArrayList<>(ticketIdList.size());
            for (Long ticketId : ticketIdList) {
                TicketMetaDto meta = ticketMetadata.get(ticketId);
                if (meta != null) metaList.add(meta);
            }
            if (metaList.isEmpty()) continue;

            AlarmMessage message = AlarmMessageFactory.ticketRelease(metaList);

            int shardNum = AlarmShardUtil.getShardNum(memberId, shardCount);
            shardMap.get(shardNum).add(new TicketReleaseSendData(memberId, message));
        }

        alarmSendService.sendToShards(
                shardMap,
                data -> alarmSendService.sendAll(data.memberId(), data.message()),
                cutoff
        );
    }

    private record TicketReleaseSendData(
            Long memberId,
            AlarmMessage message
    ) {}
}
