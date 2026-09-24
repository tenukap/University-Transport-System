package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.User;
import com.bustrans.fleettrack.repository.UserRepository;
import com.bustrans.fleettrack.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record LoginRequest(String email, String password) {}

    public record LoginResponse(String token, Long userId, String roleName, String email) {}

    public LoginResponse login(LoginRequest request) {
        String email = request.email() != null ? request.email().trim() : "";
        String password = request.password();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"));

        // TEMP: plaintext comparison for testing only — REVERT to passwordEncoder.matches() before production.
        if (password == null || !password.equals(user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String role = user.getRoleName();
        // Token subject is the userId (see JwtAuthenticationFilter); role travels as a claim.
        String token = jwtService.createToken(user.getUserId(), role);

        return new LoginResponse(token, user.getUserId(), role, user.getEmail());
    }
}
