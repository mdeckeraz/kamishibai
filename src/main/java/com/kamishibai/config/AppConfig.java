package com.kamishibai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@Configuration
@Profile("!test") // Active for all profiles except test
public class AppConfig {
    
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}