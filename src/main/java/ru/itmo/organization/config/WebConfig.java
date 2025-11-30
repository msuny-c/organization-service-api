package ru.itmo.organization.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${cors.allowed-origins:https://se.ifmo.ru}")
    private String[] allowedOrigins;
    
    @Value("${static.files.path:}")
    private String staticFilesPath;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(resolvedOrigins())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (staticFilesPath != null && !staticFilesPath.isEmpty()) {
            File staticDir = new File(staticFilesPath);
            if (staticDir.exists() && staticDir.isDirectory()) {
                registry.addResourceHandler("/**")
                        .addResourceLocations("file:" + staticFilesPath + "/")
                        .resourceChain(true);
                System.out.println("Serving static files from external directory: " + staticFilesPath);
                return;
            }
        }
        
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true);
        
        registry.addResourceHandler("/api/**")
                .addResourceLocations("classpath:/static/");
    }

    private String[] resolvedOrigins() {
        return Arrays.stream(allowedOrigins)
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }
}
