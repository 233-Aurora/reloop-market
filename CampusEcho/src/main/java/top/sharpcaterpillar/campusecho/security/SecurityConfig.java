package top.sharpcaterpillar.campusecho.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/error", "/api/v1/health", "/api/v1/auth/**", "/api/v1/categories/tree").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*/reviews").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/avatars/*").permitAll()
                        .requestMatchers("/api/v1/items/mine").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/items/*/images", "/api/v1/item-images/*/content").permitAll()
                        .requestMatchers(request -> HttpMethod.GET.matches(request.getMethod())
                                && ("/api/v1/items".equals(request.getRequestURI())
                                || request.getRequestURI().matches("/api/v1/items/\\d+")))
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.getWriter().write("{\"type\":\"https://campusecho.local/problems/forbidden\",\"title\":\"没有权限执行此操作\",\"status\":403,\"detail\":\"当前账户没有执行该操作的权限\",\"code\":\"FORBIDDEN\"}");
                        })
                        .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write("{\"type\":\"https://campusecho.local/problems/unauthorized\",\"title\":\"认证失败\",\"status\":401,\"detail\":\"需要有效的访问令牌\",\"code\":\"UNAUTHORIZED\"}");
                        }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
