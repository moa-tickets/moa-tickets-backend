package settings.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Profile("test")
@TestComponent
public class TestSecurityConfig {
    @Bean
    public OncePerRequestFilter testPrincipalInjectFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                String passHeader = request.getHeader("X-TEST-PASS-ID");
                if(passHeader != null && !passHeader.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
                    long memberId = Long.parseLong(passHeader);

                    var auth = new UsernamePasswordAuthenticationToken(
                            memberId,
                            null,
                            List.of()
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    @Order(0)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http, OncePerRequestFilter testPrincipalInjectFilter) throws IOException, ServletException {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(testPrincipalInjectFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
