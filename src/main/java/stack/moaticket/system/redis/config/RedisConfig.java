package stack.moaticket.system.redis.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import stack.moaticket.system.redis.component.ops.*;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfig {
    private final RedisProperties properties;

    @Bean("innerRedisFactory")
    public LettuceConnectionFactory innerRedisFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(properties.inner().host());
        config.setPort(properties.inner().port());
        config.setUsername(properties.inner().username());
        config.setPassword(properties.inner().password());
        return new LettuceConnectionFactory(config);
    }

    @Bean("outerRedisFactory")
    public LettuceConnectionFactory outerRedisFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(properties.outer().host());
        config.setPort(properties.outer().port());
//        config.setUsername(properties.outer().username());
//        config.setPassword(properties.outer().password());
        return new LettuceConnectionFactory(config);
    }

    @Bean("innerStringRedisTemplate")
    public StringRedisTemplate innerStringRedisTemplate(
            @Qualifier("innerRedisFactory") LettuceConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean("outerStringRedisTemplate")
    public StringRedisTemplate outerStringRedisTemplate(
            @Qualifier("outerRedisFactory") LettuceConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public RedisOpsRouter router(
            RedisBasicOps basic,
            RedisZsetOps zset,
            RedisStreamOps stream,
            RedisLuaOps lua
    ) {
        return new RedisOpsRouter(
                Map.of(
                        RedisBasicOps.class, basic,
                        RedisZsetOps.class, zset,
                        RedisStreamOps.class, stream,
                        RedisLuaOps.class, lua));
    }
}
