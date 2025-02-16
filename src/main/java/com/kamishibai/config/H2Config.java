package com.kamishibai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.h2.H2ConsoleAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

@Configuration
@Profile("!prod")
@ConditionalOnClass(org.h2.server.web.WebServlet.class)
public class H2Config extends H2ConsoleAutoConfiguration {
    // This class exists only to disable H2 console in production
}
