package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.model.Models;
import com.bustrans.fleettrack.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Models.LoginResponse login(@Valid @RequestBody Models.LoginRequest request) {
        return authService.login(request);
    }
}
