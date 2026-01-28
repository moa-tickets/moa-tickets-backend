package stack.moaticket.system.sse.component;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import settings.config.TestFixtureConfig;
import settings.config.TestSecurityConfig;
import settings.support.fixture.MemberFixture;
import settings.support.util.ReflectUtil;
import stack.moaticket.domain.member.entity.Member;
import stack.moaticket.system.alarm.sse.model.ConnectPayload;
import stack.moaticket.system.alarm.sse.model.EmitterMeta;
import stack.moaticket.system.alarm.sse.register.SseEmitterRegister;
import stack.moaticket.system.alarm.sse.service.SseHeartbeatService;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "alarm.sender=sse"
        })
@Import({TestSecurityConfig.class, TestFixtureConfig.class})
public class SseHeartbeatSchedulerE2ETest {
    @LocalServerPort int port;

    // Fixture
    @Autowired MemberFixture memberFixture;

    @Autowired ObjectMapper objectMapper;
    @Autowired SseEmitterRegister sseEmitterRegister;
    @Autowired SseHeartbeatService sseHeartbeatService;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .build();

    @AfterEach
    void clear() {
        memberFixture.clear();
    }

    @Test
    @DisplayName("SSE를 구독하면 클라이언트에서 Heartbeat 이벤트를 받는다.")
    void subscribeThenManualHeartbeatThenReceivesEvent() throws Exception {
        // given
        Member member = memberFixture.create();
        Long memberId = member.getId();

        String url = "http://localhost:" + port + "/api/alarm/sub";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "text/event-stream")
                .addHeader("X-TEST-PASS-ID", String.valueOf(memberId))
                .build();

        CountDownLatch opened = new CountDownLatch(1);
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch received = new CountDownLatch(1);
        AtomicReference<String> cidRef = new AtomicReference<>();
        AtomicReference<String> dataRef = new AtomicReference<>();

        EventSource es = EventSources.createFactory(client)
                .newEventSource(request, new EventSourceListener() {
                    @Override
                    public void onOpen(@NonNull EventSource eventSource, @NonNull Response response) {
                        opened.countDown();
                    }

                    @Override
                    public void onEvent(@NonNull EventSource eventSource, @Nullable String id, @Nullable String type, @NonNull String data) {
                        if("CONNECT".equals(type)) {
                            try {
                                ConnectPayload payload = objectMapper.readValue(data, ConnectPayload.class);
                                cidRef.set(payload.connectionId());
                                connected.countDown();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            return;
                        }
                        if("HEARTBEAT".equals(type)) {
                            dataRef.set(data);
                            received.countDown();
                        }
                    }
                });
        try {
            assertThat(opened.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(connected.await(3, TimeUnit.SECONDS)).isTrue();

            String cid = cidRef.get();
            assertThat(cid).isNotBlank();

            EmitterMeta meta = sseEmitterRegister.get(memberId, cid);
            ReflectUtil.setAtomicLong(meta, "lastSentAtMillis", System.currentTimeMillis() - 60_000);

            assertThat(meta).isNotNull();

            // when
            sseHeartbeatService.sendHeartbeat();

            // then
            assertThat(received.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(dataRef.get()).isNotBlank();
        } finally {
            es.cancel();
        }
    }
}