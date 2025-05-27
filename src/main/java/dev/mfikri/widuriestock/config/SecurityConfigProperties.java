package dev.mfikri.widuriestock.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "application.security")
@Getter
@Setter
public class SecurityConfigProperties {
    private String secretKey;
    private Long jwtTtl;
}
