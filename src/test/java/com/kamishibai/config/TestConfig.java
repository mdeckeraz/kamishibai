package com.kamishibai.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.*;

@Configuration
@Profile("test")
public class TestConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    @Primary
    public Clock fixedClock() {
        // Fix the time to 10 PM for all tests
        LocalDateTime fixedTime = LocalDateTime.of(2025, 2, 18, 22, 0);
        return Clock.fixed(
            fixedTime.atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
    }
}
