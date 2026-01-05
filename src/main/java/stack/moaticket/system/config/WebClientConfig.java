package stack.moaticket.system.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

// TODO - MVP 이후 상세 설정

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
                .clientConnector(
                        new ReactorClientHttpConnector(
                                HttpClient.create()
                                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                                        .responseTimeout(Duration.ofSeconds(5))
                        )
                )
                .build();
    }
}
