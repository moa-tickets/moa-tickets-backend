package stack.moaticket.system.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.data.redis")
public record RedisProperties(
        Config inner,
        Config outer) {
    public record Config(
            String host,
            int port,
            String username,
            String password
    ) {}
}
