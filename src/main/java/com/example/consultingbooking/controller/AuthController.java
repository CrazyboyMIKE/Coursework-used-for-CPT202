package com.example.consultingbooking.controller;

import com.example.consultingbooking.dto.AuthDtos;
import com.example.consultingbooking.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/register/customer")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.AuthResponse registerCustomer(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/register/specialist")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.AuthResponse registerSpecialist(@Valid @RequestBody AuthDtos.SpecialistRegisterRequest request) {
        return authService.registerSpecialist(request);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(AuthService.AUTH_HEADER) String token) {
        authService.logout(token);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @RequestHeader(AuthService.AUTH_HEADER) String token,
            @Valid @RequestBody AuthDtos.ChangePasswordRequest request
    ) {
        authService.changePassword(token, request);
    }

    @PostMapping("/password-reset/request")
    public AuthDtos.PasswordResetRequestResponse requestPasswordReset(
            @Valid @RequestBody AuthDtos.PasswordResetRequest request
    ) {
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(
            @Valid @RequestBody AuthDtos.PasswordResetConfirmRequest request
    ) {
        authService.confirmPasswordReset(request);
    }
}
