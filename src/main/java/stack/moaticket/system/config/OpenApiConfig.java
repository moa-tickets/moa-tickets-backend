package stack.moaticket.system.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi(){

        return new OpenAPI()
                .info(new Info()
                        .title("모아티켓 API명세서")
                        .description("모아티켓 개발 API 목록입니다.")
                        .version("1차")
                )
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("API 테스트")
                ))
                .components(new Components()
                        .addSecuritySchemes("Authorization", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("Authorization")
                        )
                )
                ;
    }
}
