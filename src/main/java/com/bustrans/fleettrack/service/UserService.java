package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.dto.UserDto.UserRequest;
import com.bustrans.fleettrack.dto.UserDto.UserResponse;
import com.bustrans.fleettrack.entity.User;
import com.bustrans.fleettrack.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    // Needed to BCrypt-hash passwords on create/update (AuthService verifies with BCrypt).
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return UserResponse.fromEntity(user);
    }

    public UserResponse createUser(UserRequest request) {
        if (request.email() != null && userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already in use: " + request.email());
        }

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                // TEMP: storing plaintext for testing only — REVERT to passwordEncoder.encode() before production.
                .passwordHash(request.password())
                .phone(request.phone())
                .roleName(request.roleName())
                .accountStatus("Active")
                .createdAt(now)
                .updatedAt(now)
                .build();
        return UserResponse.fromEntity(userRepository.save(user));
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.email() != null) {
            userRepository.findByEmail(request.email())
                    .filter(existing -> !existing.getUserId().equals(id))
                    .ifPresent(existing -> { throw new RuntimeException("Email already in use"); });
            user.setEmail(request.email());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.roleName() != null) {
            user.setRoleName(request.roleName());
        }
        user.setUpdatedAt(LocalDateTime.now());
        return UserResponse.fromEntity(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
