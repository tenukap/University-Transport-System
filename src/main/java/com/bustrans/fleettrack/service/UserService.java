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
                .orElseThrow(() -> new IllegalArgumentException("User " + id + " was not found"));
        return UserResponse.fromEntity(user);
    }

    public UserResponse createUser(UserRequest request) {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
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
                .orElseThrow(() -> new IllegalArgumentException("User " + id + " was not found"));

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.email() != null) {
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
            throw new IllegalArgumentException("User " + id + " was not found");
        }
        userRepository.deleteById(id);
    }
}
