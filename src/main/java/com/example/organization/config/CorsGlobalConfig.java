package com.example.organization.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsGlobalConfig {

  @Value("${cors.allowed-origins:https://se.ifmo.ru}")
  private String[] allowedOrigins;

  @Bean
  public FilterRegistrationBean<CorsFilter> corsFilter() {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowCredentials(true);
    c.setAllowedOrigins(resolveAllowedOrigins());
    c.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    c.setAllowedHeaders(List.of("*"));
    c.setExposedHeaders(List.of("Location","Content-Disposition"));
    c.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
    s.registerCorsConfiguration("/**", c);

    FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(s));
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
  }

  private List<String> resolveAllowedOrigins() {
    return Arrays.stream(allowedOrigins)
        .map(String::trim)
        .filter(origin -> !origin.isBlank())
        .collect(Collectors.toList());
  }
}
