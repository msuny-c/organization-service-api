package ru.itmo.organization;

import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRetry(order = Ordered.HIGHEST_PRECEDENCE, proxyTargetClass = true)
public class OrganizationManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrganizationManagementApplication.class, args);
    }
}
