package com.edu.platform.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code app.jwt.*} block of application.yml.
 */
@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;
    private long expireMinutes = 1440;
    private String issuer = "online-education-platform";
}
