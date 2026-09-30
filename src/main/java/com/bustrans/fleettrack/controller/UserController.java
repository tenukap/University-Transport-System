package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.UserDto.UserRequest;
import com.bustrans.fleettrack.dto.UserDto.UserResponse;
import com.bustrans.fleettrack.dto.UserDto.UserSelfUpdateRequest;
import com.bustrans.fleettrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    /**
     * ADMIN: full update via UserRequest.
     * STUDENT: may only update their own record (id == JWT userId) using fullName + phone only.
     * Both callers receive the same UserResponse JSON shape.
     */
    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id,
                                   @RequestBody Map<String, String> body,
                                   Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            UserRequest request = new UserRequest(
                    body.get("fullName"), body.get("email"), body.get("password"),
                    body.get("phone"), body.get("roleName"), body.get("accountStatus"));
            return userService.updateUser(id, request);
        }

        // STUDENT path: self-only, fullName + phone only.
        try {
            Long callerId = Long.parseLong(authentication.getName());
            if (!callerId.equals(id)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Students may only update their own profile");
            }
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        }
        return userService.updateUserSelf(id, new UserSelfUpdateRequest(body.get("fullName"), body.get("phone")));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
