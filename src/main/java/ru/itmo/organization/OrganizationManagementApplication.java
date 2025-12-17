package ru.itmo.organization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableRetry(order = Ordered.HIGHEST_PRECEDENCE, proxyTargetClass = true)
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE + 1)
public class OrganizationManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrganizationManagementApplication.class, args);
    }
}
