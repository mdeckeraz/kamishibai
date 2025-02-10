package com.kamishibai.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.*;

@TestConfiguration
public class TestConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Clock clock() {
        // Fix the clock to 2025-02-09 23:15:00 for consistent testing
        return Clock.fixed(
            LocalDateTime.of(2025, 2, 9, 23, 15, 0).toInstant(ZoneOffset.UTC),
            ZoneId.systemDefault()
        );
    }
}
