package com.corum.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * migration 프로파일에서 레거시(MS SQL Server) 조회용 보조 데이터소스를 추가한다.
 * 주의: DataSource 빈을 하나라도 직접 정의하면 Boot의 DataSourceAutoConfiguration
 * (@ConditionalOnMissingBean(DataSource.class))이 통째로 비활성화되므로,
 * Corum 기본(PostgreSQL) 데이터소스도 여기서 @Primary로 함께 재정의해야 한다.
 */
@Configuration
@Profile("migration")
public class LegacyDataSourceConfig {

    @Value("${legacy.datasource.url}")
    private String legacyUrl;

    @Value("${legacy.datasource.username}")
    private String legacyUsername;

    @Value("${legacy.datasource.password}")
    private String legacyPassword;

    // ===== Corum 기본 데이터소스 (PostgreSQL) — Boot 자동설정 대체 =====

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // ===== 레거시 데이터소스 (MS SQL Server) =====

    @Bean
    public DataSource legacyDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(legacyUrl);
        config.setUsername(legacyUsername);
        config.setPassword(legacyPassword);
        config.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        config.setMaximumPoolSize(2);
        config.setReadOnly(true);
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate legacyJdbcTemplate(@Qualifier("legacyDataSource") DataSource legacyDataSource) {
        return new JdbcTemplate(legacyDataSource);
    }
}
