package ru.itmo.organization.service;

import jakarta.validation.Valid;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.organization.dto.AuthRequest;
import ru.itmo.organization.dto.AuthResponse;
import ru.itmo.organization.model.UserAccount;
import ru.itmo.organization.model.UserRole;
import ru.itmo.organization.repository.UserAccountRepository;
import ru.itmo.organization.security.JwtService;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(@Valid AuthRequest request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername().trim());
        String salt = generateSalt();
        user.setSalt(salt);
        user.setPassword(hashPassword(request.getPassword(), salt));
        user.setRole(UserRole.USER);
        userAccountRepository.save(user);

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getUsername(), user.getRole());
    }

    public AuthResponse login(@Valid AuthRequest request) {
        UserAccount user = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Неверные учетные данные"));

        if (!passwordEncoder.matches(request.getPassword() + user.getSalt(), user.getPassword())) {
            throw new BadCredentialsException("Неверные учетные данные");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getUsername(), user.getRole());
    }

    private String hashPassword(String rawPassword, String salt) {
        return passwordEncoder.encode(rawPassword + salt);
    }

    private String generateSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
