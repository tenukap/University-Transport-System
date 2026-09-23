package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.User;
import com.bustrans.fleettrack.repository.UserRepository;
import com.bustrans.fleettrack.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/portal")
public class PortalController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public PortalController(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record PortalLoginRequest(String email, String password) {}

    public record PortalLoginResponse(String token, Long userId, String roleName, String email) {}

    public record PortalMessageResponse(String message) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody PortalLoginRequest request) {
        String email = request.email() != null ? request.email().trim() : "";
        String password = request.password();

        if (email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new PortalMessageResponse("Email and password are required"));
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new PortalMessageResponse("You cannot enter this system. Your account has not been added by the admin."));
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new PortalMessageResponse("Invalid email or password"));
        }

        if (!"Active".equalsIgnoreCase(user.getAccountStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new PortalMessageResponse("Account is " + user.getAccountStatus() + ". Please contact the transport office."));
        }

        String role = user.getRoleName();
        String token = jwtService.createToken(user.getUserId(), role);
        return ResponseEntity.ok(new PortalLoginResponse(token, user.getUserId(), role, user.getEmail()));
    }
}
