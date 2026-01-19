package stack.moaticket.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import stack.moaticket.application.component.register.AlarmEmitterRegister;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.domain.member.service.MemberService;
import stack.moaticket.domain.member.type.MemberState;
import stack.moaticket.domain.session_start_alarm.entity.SessionStartAlarm;
import stack.moaticket.system.component.Validator;
import stack.moaticket.system.exception.MoaExceptionType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {
    private final Validator validator;

    private final MemberService memberService;

    private final AlarmEmitterRegister alarmEmitterRegister;
    private static final Long EXPIRE_TIME = 24 * 60 * 60 * 1000L;

    public SseEmitter subscribe(Long memberId) {
        validator.of(memberService.findById(memberId))
                .validateOrThrow(Objects::isNull, MoaExceptionType.MEMBER_NOT_FOUND)
                .validateOrThrow(m -> m.getState() != MemberState.ACTIVE, MoaExceptionType.UNAUTHORIZED);

        SseEmitter emitter = new SseEmitter(EXPIRE_TIME);
        alarmEmitterRegister.insert(memberId, emitter);

        try {
            emitter.send(SseEmitter
                    .event()
                    .data("connected"));
        } catch (IOException | IllegalStateException e) {
            alarmEmitterRegister.remove(memberId, emitter);
            emitter.complete();
            log.error("연결 수립에 실패하였습니다.");
        }

        Runnable cleanup = () -> alarmEmitterRegister.remove(memberId, emitter);

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    public List<List<SessionStartAlarm>> sendConcertStartInform(List<SessionStartAlarm> candidateList) {
        List<List<SessionStartAlarm>> ret = new ArrayList<>();
        List<SessionStartAlarm> succeededList = new ArrayList<>();
        List<SessionStartAlarm> disconnectedList = new ArrayList<>();

        for(SessionStartAlarm candidate : candidateList) {
            List<SseEmitter> emitterList = alarmEmitterRegister.getSseEmitters(candidate.getMember().getId());
            if(emitterList == null || emitterList.isEmpty()) {
                disconnectedList.add(candidate);
                continue;
            }

            String prefix = candidate.getType().getPrefix();
            String eventName = candidate.getType().getName();
            for(SseEmitter emitter : emitterList) {
                try {
                    emitter.send(SseEmitter
                            .event()
                            .id(prefix + candidate.getId())
                            .name(eventName)
                            .data(new ConcertData(
                                    candidate.getAlarmAt(),
                                    candidate.getSession().getDate(),
                                    candidate.getType().getName(),
                                    candidate.getSession().getConcert().getName())));
                } catch (IOException | IllegalStateException e) {
                    alarmEmitterRegister.remove(candidate.getMember().getId(), emitter);
                    try { emitter.complete(); } catch (Exception ignore) {}
                }
            }

            succeededList.add(candidate);
        }

        ret.add(succeededList);
        ret.add(disconnectedList);

        return ret;
    }

    private record ConcertData(LocalDateTime alarmAt, LocalDateTime startAt, String alarmType, String concertName) {}
}
