package com.kamishibai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@Configuration
public class ClockConfig {
    
    @Bean
    @Profile("!test") // Use this bean for all profiles except test
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
