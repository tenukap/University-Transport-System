package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.PortalLoginDto.PortalLoginRequest;
import com.bustrans.fleettrack.dto.PortalLoginDto.PortalLoginResponse;
import com.bustrans.fleettrack.dto.PortalLoginDto.PortalMessageResponse;
import com.bustrans.fleettrack.dto.UserDto.UserResponse;
import com.bustrans.fleettrack.repository.UserRepository;
import com.bustrans.fleettrack.repository.UserRepository.UserRecord;
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

    public PortalController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody PortalLoginRequest request) {
        String email = request.getEffectiveEmail();
        String password = request.password();

        if (email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new PortalMessageResponse("Email and password are required"));
        }

        Optional<UserRecord> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new PortalMessageResponse("You cannot enter this system. Your account has not been added by the admin."));
        }

        UserRecord user = userOpt.get();

        boolean passMatch = false;
        try {
            passMatch = passwordEncoder.matches(password, user.passwordHash());
        } catch (Exception ignored) {}
        if (!passMatch && password.equals(user.passwordHash())) {
            passMatch = true;
        }

        if (!passMatch) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new PortalMessageResponse("Invalid email or password"));
        }

        String roleName = user.roleName() != null ? user.roleName().trim() : "";
        String normalizedRole = roleName.toUpperCase().replace('-', '_').replace(' ', '_');
        if ("FINANCEOFFICER".equals(normalizedRole)) normalizedRole = "FINANCE_OFFICER";

        if (!"Active".equalsIgnoreCase(user.accountStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new PortalMessageResponse("Account is " + user.accountStatus() + ". Please contact the transport office."));
        }

        String token = jwtService.createToken(user.userId(), normalizedRole);
        return ResponseEntity.ok(new PortalLoginResponse(token, UserResponse.fromRecord(user)));
    }
}
