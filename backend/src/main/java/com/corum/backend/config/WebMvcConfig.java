package com.corum.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // 로컬 개발용 오리진은 기본값으로 항상 포함하고, 운영에서 프론트를 별도 도메인
    // (예: Cloudflare Pages)으로 배포할 때는 CORS_ALLOWED_ORIGINS 환경변수로 추가한다.
    // nginx가 프론트/백엔드를 같은 오리진으로 묶어주던 기존 운영 배포와 달리,
    // Pages+Tunnel처럼 완전히 다른 도메인끼리 붙는 구성에서는 이 설정이 필수다.
    @Value("${app.cors.allowed-origins:}")
    private String extraAllowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = new ArrayList<>(List.of(
                "http://localhost:5173",   // Vue dev server
                "http://localhost:3000"
        ));
        if (extraAllowedOrigins != null && !extraAllowedOrigins.isBlank()) {
            for (String origin : extraAllowedOrigins.split(",")) {
                if (!origin.isBlank()) origins.add(origin.trim());
            }
        }

        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
