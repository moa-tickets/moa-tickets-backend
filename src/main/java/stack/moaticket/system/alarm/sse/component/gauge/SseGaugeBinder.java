package stack.moaticket.system.alarm.sse.component.gauge;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import stack.moaticket.system.alarm.sse.component.register.SseEmitterRegister;

import java.util.concurrent.ThreadPoolExecutor;

import static stack.moaticket.system.alarm.sse.component.gauge.SseGaugeMetrics.*;

public class SseGaugeBinder {
    public SseGaugeBinder(
            MeterRegistry meterRegistry,
            SseEmitterRegister sseEmitterRegister,
            @Qualifier("asyncExecutor") AsyncTaskExecutor asyncExecutor) {

        // 전체 SSE 연결 수
        Gauge.builder(SSE_EMITTER_TOTAL, sseEmitterRegister, SseEmitterRegister::getTotalEmitterCount)
                .description("Total number of active SSE emitters")
                .register(meterRegistry);

        // 전체 연결된 유저 수
        Gauge.builder(SSE_MEMBER_TOTAL, sseEmitterRegister, SseEmitterRegister::getTotalMemberCount)
                .description("Total number of active members")
                .register(meterRegistry);

        if(asyncExecutor instanceof ThreadPoolTaskExecutor executor) {
            ThreadPoolExecutor tp = executor.getThreadPoolExecutor();

            // 활성화 된 SSE Executor 스레드
            Gauge.builder(SSE_EXECUTOR_ACTIVE, tp, ThreadPoolExecutor::getActiveCount)
                    .description("Active SSE executor threads")
                    .register(meterRegistry);

            // SSE Executor의 Queue Size
            Gauge.builder(SSE_EXECUTOR_QUEUE_SIZE, tp, e -> e.getQueue().size())
                    .description("SSE executor queue size")
                    .register(meterRegistry);

            // SSE Executor의 Pool Size
            Gauge.builder(SSE_EXECUTOR_POOL_SIZE, tp, ThreadPoolExecutor::getPoolSize)
                    .description("SSE executor pool size")
                    .register(meterRegistry);
        }

    }
}
