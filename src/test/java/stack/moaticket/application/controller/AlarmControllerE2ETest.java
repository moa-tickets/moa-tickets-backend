package stack.moaticket.application.controller;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import settings.config.TestSecurityConfig;
import settings.model.SseEvent;
import stack.moaticket.system.alarm.sse.model.ConnectPayload;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "alarm.sender=sse"
        })
@Import(TestSecurityConfig.class)
@Sql(scripts = "/sql/member_01_not_seller.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class AlarmControllerE2ETest {
    @LocalServerPort int port;

    @Autowired ObjectMapper objectMapper;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .build();

    @Test
    @DisplayName("연결에 성공하면 CONNECT 이벤트 메시지를 받는다.")
    void subscribeThenReceivesConnectEvent() throws Exception {
        // given
        String url = "http://localhost:" + port + "/api/alarm/sub";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "text/event-stream")
                .addHeader("X-TEST-PASS-ID", "1")
                .build();

        // when
        try(Response response = client.newCall(request).execute()) {
            // then
            assertThat(response.code()).isEqualTo(200);

            String contentType = response.header("Content-Type");
            assertThat(contentType).contains("text/event-stream");

            BufferedSource source = response.body().source();
            SseEvent event = SseEvent.readNextSseEvent(source, Duration.ofSeconds(3));

            assertThat(event.getEvent()).isEqualTo("CONNECT");

            ConnectPayload payload = objectMapper.readValue(event.getData(), ConnectPayload.class);
            String cid = payload.connectionId();
            assertThat(cid).isNotBlank();
        }
    }
}
