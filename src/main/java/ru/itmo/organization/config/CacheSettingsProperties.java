package ru.itmo.organization.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cache.statistics")
public class CacheSettingsProperties {
    private boolean loggingEnabled = false;
}
