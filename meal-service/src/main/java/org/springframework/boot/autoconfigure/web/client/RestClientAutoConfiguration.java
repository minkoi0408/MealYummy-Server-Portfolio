package org.springframework.boot.autoconfigure.web.client;

import org.springframework.context.annotation.Configuration;

/**
 * Dummy class to fix ClassNotFoundException in Spring AI 1.0.0 caused by Spring Boot 4.0.3
 * moving or removing RestClientAutoConfiguration.
 * OpenAiAutoConfiguration has an @AutoConfigureAfter that strictly requires this class to exist.
 */
@Configuration
public class RestClientAutoConfiguration {
}
