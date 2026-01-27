package stack.moaticket.system.sse.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import stack.moaticket.system.alarm.sse.component.SseHeartbeatScheduler;
import stack.moaticket.system.alarm.sse.config.SseConfig;
import stack.moaticket.system.alarm.sse.register.SseEmitterRegister;
import stack.moaticket.system.alarm.sse.service.SseHeartbeatService;
import stack.moaticket.system.alarm.sse.service.SseSubscribeService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class SseConfigTest {

    @Test
    @DisplayName("alarm.sender property가 없을 때 SSE 관련 Bean이 생성된다.")
    void ifNoPropertyThenSseBeansCreated() {
        // given
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(SseConfig.class);

        // when & then
        runner.run(context -> {
            assertThat(context).hasSingleBean(SseEmitterRegister.class);
            assertThat(context).hasSingleBean(SseHeartbeatScheduler.class);
            assertThat(context).hasSingleBean(SseSubscribeService.class);
            assertThat(context).hasSingleBean(SseHeartbeatService.class);
            assertThat(context).hasBean("syncSseSendService");
            assertThat(context).hasBean("asyncSseSendService");
            assertThat(context).hasBean("heartbeatInformScheduler");
            assertThat(context).hasBean("heartbeatExecutor");
        });
    }

    @Test
    @DisplayName("alarm.sender property가 'sse'일 때 SSE 관련 Bean이 생성된다.")
    void ifPropertyIsSseThenSseBeansCreated() {
        // given
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(SseConfig.class);

        // when & then
        runner
                .withPropertyValues("alarm.sender=sse")
                .run(context -> {
                    assertThat(context).hasSingleBean(SseEmitterRegister.class);
                    assertThat(context).hasSingleBean(SseHeartbeatScheduler.class);
                    assertThat(context).hasSingleBean(SseSubscribeService.class);
                    assertThat(context).hasSingleBean(SseHeartbeatService.class);
                    assertThat(context).hasBean("syncSseSendService");
                    assertThat(context).hasBean("asyncSseSendService");
                    assertThat(context).hasBean("heartbeatInformScheduler");
                    assertThat(context).hasBean("heartbeatExecutor");
                });
    }

    @Test
    @DisplayName("alarm.sender property가 ''일 때 Bean이 생성되지 않는다.")
    void ifPropertyIsNullThenNoBeansAreCreated() {
        // given
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(SseConfig.class);

        // when & then
        runner
                .withPropertyValues("alarm.sender=")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SseEmitterRegister.class);
                    assertThat(context).doesNotHaveBean(SseHeartbeatScheduler.class);
                    assertThat(context).doesNotHaveBean(SseSubscribeService.class);
                    assertThat(context).doesNotHaveBean(SseHeartbeatService.class);
                    assertThat(context).doesNotHaveBean("syncSseSendService");
                    assertThat(context).doesNotHaveBean("asyncSseSendService");
                    assertThat(context).doesNotHaveBean("heartbeatInformScheduler");
                    assertThat(context).doesNotHaveBean("heartbeatExecutor");
                });
    }

    @Test
    @DisplayName("백프레셔 상황일 때 뒷 작업은 Drop된다.")
    void whenSaturatedThenTasksAreDropped() throws Exception {
        // given
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(2);
        ex.setQueueCapacity(0);

        AtomicInteger rejected = new AtomicInteger(0);
        ex.setRejectedExecutionHandler((r, executor) -> rejected.incrementAndGet());
        ex.initialize();

        CountDownLatch block = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(2);

        ex.execute(() -> {
            started.countDown();
            await(block);
        });
        ex.execute(() -> {
            started.countDown();
            await(block);
        });

        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

        // when
        for(int i=0; i<50; i++) {
            ex.execute(() -> {});
        }

        // then
        assertThat(rejected.get()).isGreaterThan(0);

        block.countDown();
        ex.shutdown();
        ex.getThreadPoolExecutor().awaitTermination(1, TimeUnit.SECONDS);
    }

    private static void await(CountDownLatch latch) {
        try { latch.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
