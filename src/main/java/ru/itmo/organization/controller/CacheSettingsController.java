package ru.itmo.organization.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.itmo.organization.config.CacheSettingsProperties;

import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheSettingsController {

    private final CacheSettingsProperties properties;

    @PostMapping("/statistics/logging")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> toggleLogging(@RequestParam boolean enabled) {
        properties.setLoggingEnabled(enabled);
        return ResponseEntity.ok(Map.of("loggingEnabled", properties.isLoggingEnabled()));
    }

    @GetMapping("/statistics/logging")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("loggingEnabled", properties.isLoggingEnabled()));
    }
}
