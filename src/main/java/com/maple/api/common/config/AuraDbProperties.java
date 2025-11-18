package com.maple.api.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ConfigurationProperties(prefix = "auradb")
public class AuraDbProperties {

    private String uri;
    private String username;
    private String password;

    public boolean isConfigured() {
        return StringUtils.hasText(uri) && StringUtils.hasText(username) && StringUtils.hasText(password);
    }
}
