package stack.moaticket.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // 별도의 Bean 등록 없이 @CreatedDate, @LastModifiedDate 사용 가능
}