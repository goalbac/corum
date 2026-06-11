package com.corum.backend.config;

import com.corum.backend.security.JwtAuthFilter;
import com.corum.backend.security.JwtProvider;
import com.corum.backend.service.auth.TokenSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final VisitLoggingFilter visitLoggingFilter;
    private final TokenSessionService tokenSessionService;

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
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // 공개 API
                        .requestMatchers(HttpMethod.GET,
                                "/api/calendars",
                                "/api/calendars/events"
                        ).permitAll()
                        .requestMatchers(
                                "/api/health",
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/verify-email",
                                "/api/auth/request-password-reset",
                                "/api/auth/reset-password",
                                "/api/terms/active",
                                "/api/menus",
                                "/api/inquiries",
                                "/api/display/popups/active",
                                "/api/display/banners/active",
                                "/api/site/public",
                                "/api/files/profile/**",
                                "/api/files/inline/**",
                                "/api/files/site/**",
                                "/api/files/*/view",
                                "/api/files/*/thumbnail"
                        ).permitAll()
                        // 나머지 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(visitLoggingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
