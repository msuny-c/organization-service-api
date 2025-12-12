package ru.itmo.organization.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.organization.dto.AuthRequest;
import ru.itmo.organization.dto.AuthResponse;
import ru.itmo.organization.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/assume-admin")
    public ResponseEntity<AuthResponse> assumeAdmin(Authentication authentication) {
        return ResponseEntity.ok(authService.assumeRole(authentication, ru.itmo.organization.model.UserRole.ADMIN));
    }

    @PostMapping("/assume-user")
    public ResponseEntity<AuthResponse> assumeUser(Authentication authentication) {
        return ResponseEntity.ok(authService.assumeRole(authentication, ru.itmo.organization.model.UserRole.USER));
    }
}
