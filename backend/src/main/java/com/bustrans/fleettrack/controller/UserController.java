package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.UserDto.CreateUserRequest;
import com.bustrans.fleettrack.dto.UserDto.UpdateUserRequest;
import com.bustrans.fleettrack.dto.UserDto.UserResponse;
import com.bustrans.fleettrack.repository.UserRepository;
import com.bustrans.fleettrack.repository.UserRepository.UserRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_OFFICER')")
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromRecord)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        String name = request.getEffectiveName();
        String email = request.email() != null ? request.email().trim() : "";
        String password = request.password();
        String roleName = request.getEffectiveRole();

        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
        }
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (roleName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role name is required");
        }

        Integer roleId = userRepository.findRoleIdByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid role name: " + roleName + ". Must be Admin, FinanceOfficer, Student, or Driver"));

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with email '" + email + "' already exists");
        }

        String passwordHash = passwordEncoder.encode(password);
        UserRecord created = userRepository.save(name, email, passwordHash, roleId, "Active");

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromRecord(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(@PathVariable long id, @RequestBody UpdateUserRequest request) {
        UserRecord existing = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + id + " was not found"));

        Integer roleId = null;
        String effectiveRole = request.getEffectiveRole();
        if (effectiveRole != null && !effectiveRole.isBlank()) {
            roleId = userRepository.findRoleIdByName(effectiveRole)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid role name: " + effectiveRole + ". Must be Admin, FinanceOfficer, Student, or Driver"));
        }

        String effectiveName = request.getEffectiveName();
        String effectiveStatus = request.getEffectiveStatus();

        boolean updated = userRepository.update(id, effectiveName, roleId, effectiveStatus);
        if (!updated && roleId == null && effectiveName == null && effectiveStatus == null) {
            return UserResponse.fromRecord(existing);
        }

        return userRepository.findById(id)
                .map(UserResponse::fromRecord)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + id + " was not found"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable long id) {
        boolean deleted = userRepository.deleteById(id);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + id + " was not found");
        }
    }
}
