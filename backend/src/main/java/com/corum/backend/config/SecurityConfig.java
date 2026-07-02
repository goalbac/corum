package com.corum.backend.config;

import com.corum.backend.security.JwtAuthFilter;
import com.corum.backend.security.JwtProvider;
import com.corum.backend.service.auth.TokenSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final VisitLoggingFilter visitLoggingFilter;
    private final TokenSessionService tokenSessionService;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtProvider, userDetailsService, tokenSessionService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        // 공개 API
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/signup",
                                "/api/auth/check",
                                "/api/auth/request-password-reset",
                                "/api/auth/reset-password"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/auth/verify-email",
                                "/api/auth/check"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/calendars",
                                "/api/calendars/events"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/page-view").permitAll()
                        // 문의 접수는 비로그인 가능(POST)하지만 목록/상세/처리(GET 등)는 관리자 전용
                        .requestMatchers(HttpMethod.POST, "/api/inquiries").permitAll()
                        .requestMatchers(
                                "/api/health",
                                "/api/terms/active",
                                "/api/menus",
                                "/api/display/popups/active",
                                "/api/display/banners/active",
                                "/api/site/public",
                                "/api/files/profile/**",
                                "/api/files/inline/**",
                                "/api/files/inline-thumb/**",
                                "/api/files/site/**",
                                "/api/files/popup/**",
                                "/api/files/*/view",
                                "/api/files/*/thumbnail",
                                "/api/files/*/small-thumb"
                        ).permitAll()
                        // 관리자 패널 API는 ADMIN 그룹 소속만 허용
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 나머지 인증 필요
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write("{\"success\":false,\"message\":\"접근 권한이 없습니다.\"}");
                }))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(visitLoggingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
