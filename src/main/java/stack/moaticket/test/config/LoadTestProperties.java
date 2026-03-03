package stack.moaticket.test.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.server.test.load-test")
public record LoadTestProperties(boolean enabled) {}