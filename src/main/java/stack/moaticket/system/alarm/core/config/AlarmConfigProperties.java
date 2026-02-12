package stack.moaticket.system.alarm.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import stack.moaticket.system.alarm.core.type.SenderType;

@ConfigurationProperties(prefix = "app.server.alarm")
public record AlarmConfigProperties(
        SenderType type,
        int shardCount
) {}
